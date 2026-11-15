package com.suspended.music.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.suspended.music.data.model.AudioTags
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class TagRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val writeLock = ReentrantLock()
    private val tempFiles = mutableListOf<File>()

    companion object {
        private const val MAX_ARTWORK_BYTES = 10 * 1024 * 1024L
        private const val ARTWORK_TMP_DIR = "artwork_tmp"
    }

    fun readTags(path: String): Result<AudioTags> {
        // Drop artwork temp files from the previous read; the UI has moved on.
        cleanupTempFiles()
        return try {
            val file = File(path)
            if (!file.exists()) return Result.failure(Exception("File not found"))

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag

            if (tag == null) {
                return Result.success(AudioTags())
            }

            var artworkUri: Uri? = null
            val artworkList = tag.artworkList
            if (artworkList != null && artworkList.isNotEmpty()) {
                try {
                    val artwork = artworkList[0]
                    val bytes = artwork.binaryData
                    if (bytes != null && bytes.size <= MAX_ARTWORK_BYTES) {
                        val dir = File(context.cacheDir, ARTWORK_TMP_DIR).apply { mkdirs() }
                        val name = "art_${SecureRandom().nextInt(Int.MAX_VALUE)}.jpg"
                        val tempFile = File(dir, name)
                        FileOutputStream(tempFile).use { it.write(bytes) }
                        artworkUri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", tempFile
                        )
                        synchronized(tempFiles) { tempFiles.add(tempFile) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val tags = AudioTags(
                title = tag.getFirst(FieldKey.TITLE) ?: "",
                artist = tag.getFirst(FieldKey.ARTIST) ?: "",
                album = tag.getFirst(FieldKey.ALBUM) ?: "",
                year = tag.getFirst(FieldKey.YEAR) ?: "",
                genre = tag.getFirst(FieldKey.GENRE) ?: "",
                trackNumber = tag.getFirst(FieldKey.TRACK) ?: "",
                discNumber = tag.getFirst(FieldKey.DISC_NO) ?: "",
                composer = tag.getFirst(FieldKey.COMPOSER) ?: "",
                comment = tag.getFirst(FieldKey.COMMENT) ?: "",
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST) ?: "",
                lyrics = tag.getFirst(FieldKey.LYRICS) ?: "",
                artworkUri = artworkUri
            )
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun writeTags(path: String, tags: AudioTags): Result<Boolean> {
        // Serialize writes to prevent concurrent file corruption
        return writeLock.withLock {
            try {
                val file = runCatching { File(path).canonicalFile }.getOrNull()
                    ?: return Result.failure(IllegalArgumentException("Invalid path"))
                // This is a write primitive: only ever touch shared media storage,
                // never an arbitrary path handed in by a caller.
                if (!isInMediaStorage(file)) {
                    return Result.failure(
                        SecurityException("Refusing to write outside shared media storage")
                    )
                }
                if (!file.exists()) return Result.failure(Exception("File not found"))
                
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault
                
                tag.setField(FieldKey.TITLE, tags.title)
                tag.setField(FieldKey.ARTIST, tags.artist)
                tag.setField(FieldKey.ALBUM, tags.album)
                tag.setField(FieldKey.YEAR, tags.year)
                tag.setField(FieldKey.GENRE, tags.genre)
                tag.setField(FieldKey.LYRICS, tags.lyrics)
                // Numeric fields: write the value only when the editor has one,
                // so an untouched field can never wipe the file's value.
                if (tags.trackNumber.isNotBlank()) {
                    tag.setField(FieldKey.TRACK, tags.trackNumber)
                }
                if (tags.discNumber.isNotBlank()) {
                    tag.setField(FieldKey.DISC_NO, tags.discNumber)
                }
                if (tags.composer.isNotBlank()) {
                    tag.setField(FieldKey.COMPOSER, tags.composer)
                }
                if (tags.comment.isNotBlank()) {
                    tag.setField(FieldKey.COMMENT, tags.comment)
                }
                if (tags.albumArtist.isNotBlank()) {
                    tag.setField(FieldKey.ALBUM_ARTIST, tags.albumArtist)
                }
                
                if (tags.hasArtworkChanged) {
                    tag.deleteArtworkField()
                    if (tags.artworkUri != null) {
                        try {
                            val artworkUri = tags.artworkUri
                            // Only content URIs — a file:// path would let a caller
                            // pull an arbitrary readable file into the tag.
                            if (artworkUri.scheme != ContentResolver.SCHEME_CONTENT) {
                                throw IllegalArgumentException("Artwork must be a content:// URI")
                            }
                            val mime = context.contentResolver.getType(artworkUri)
                            if (mime != null && !mime.startsWith("image/")) {
                                throw IllegalArgumentException("Artwork must be an image (was $mime)")
                            }
                            val bytes = context.contentResolver.openInputStream(artworkUri)?.use { stream ->
                                val buf = stream.readBytesCapped(MAX_ARTWORK_BYTES)
                                    ?: throw IllegalArgumentException("Artwork too large (max ${MAX_ARTWORK_BYTES / 1024 / 1024}MB)")
                                buf
                            }
                            if (bytes != null) {
                                // Don't trust the declared MIME type — verify the bytes.
                                if (!looksLikeImage(bytes)) {
                                    throw IllegalArgumentException("Artwork data is not a recognised image")
                                }
                                val artwork = org.jaudiotagger.tag.images.StandardArtwork()
                                artwork.binaryData = bytes
                                tag.setField(artwork)
                            }
                        } catch (e: Exception) {
                            // Artwork failure must not be reported as a successful save.
                            return Result.failure(e)
                        }
                    }
                }
                
                AudioFileIO.write(audioFile)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Roots considered "shared media storage" for tag writes. */
    private fun mediaStorageRoots(): List<String> {
        val roots = LinkedHashSet<String>()
        runCatching { Environment.getExternalStorageDirectory()?.canonicalPath }
            .getOrNull()?.let { roots.add(it) }
        context.getExternalFilesDirs(null).forEach { dir ->
            // .../Android/data/<pkg>/files → the volume root (primary or SD card)
            val volumeRoot = dir?.parentFile?.parentFile?.parentFile?.parentFile
            runCatching { volumeRoot?.canonicalPath }.getOrNull()?.let { roots.add(it) }
        }
        return roots.toList()
    }

    private fun isInMediaStorage(file: File): Boolean {
        val path = file.path
        return mediaStorageRoots().any { root -> path.startsWith("$root${File.separator}") }
    }

    /**
     * Magic-byte sniff so a mislabelled content URI can't inject arbitrary bytes
     * as embedded artwork.
     */
    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        fun at(offset: Int, vararg expected: Int): Boolean =
            expected.indices.all { (bytes[offset + it].toInt() and 0xFF) == expected[it] }

        return at(0, 0xFF, 0xD8, 0xFF) ||                                     // JPEG
            at(0, 0x89, 0x50, 0x4E, 0x47) ||                                  // PNG
            at(0, 0x47, 0x49, 0x46, 0x38) ||                                  // GIF8
            at(0, 0x42, 0x4D) ||                                              // BMP
            (at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50))  // RIFF/WEBP
    }

    fun cleanupTempFiles() {
        synchronized(tempFiles) {
            tempFiles.forEach { file ->
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) { }
            }
            tempFiles.clear()
        }
    }

    private fun java.io.InputStream.readBytesCapped(limit: Long): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = read(buf)
            if (n == -1) break
            total += n
            if (total > limit) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
