package com.suspended.music.data.repository

import android.content.Context
import android.net.Uri
import com.suspended.music.data.model.AudioTags
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class TagRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Guards concurrent file writes to prevent corruption. */
    private val writeLock = ReentrantLock()

    /** Tracks temp artwork files so they can be cleaned up. */
    private val tempFiles = mutableListOf<File>()

    fun readTags(path: String): Result<AudioTags> {
        return try {
            val file = File(path)
            if (!file.exists()) return Result.failure(Exception("File not found"))
            
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            
            if (tag == null) {
                return Result.success(AudioTags())
            }
            
            // Extract Artwork to temp file
            var artworkUri: Uri? = null
            val artworkList = tag.artworkList
            if (artworkList != null && artworkList.isNotEmpty()) {
                try {
                    val artwork = artworkList[0]
                    val bytes = artwork.binaryData
                    if (bytes != null) {
                        val tempFile = File(context.cacheDir, "art_temp_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { it.write(bytes) }
                        artworkUri = Uri.fromFile(tempFile)
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
                val file = File(path)
                if (!file.exists()) return Result.failure(Exception("File not found"))
                
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault
                
                tag.setField(FieldKey.TITLE, tags.title)
                tag.setField(FieldKey.ARTIST, tags.artist)
                tag.setField(FieldKey.ALBUM, tags.album)
                tag.setField(FieldKey.YEAR, tags.year)
                tag.setField(FieldKey.GENRE, tags.genre)
                tag.setField(FieldKey.LYRICS, tags.lyrics)
                
                // Handle Artwork
                if (tags.hasArtworkChanged) {
                    tag.deleteArtworkField()
                    if (tags.artworkUri != null) {
                        try {
                            val bytes = context.contentResolver.openInputStream(tags.artworkUri)?.use { it.readBytes() }
                            if (bytes != null) {
                                val artwork = org.jaudiotagger.tag.images.StandardArtwork()
                                artwork.binaryData = bytes
                                tag.setField(artwork)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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

    /**
     * Deletes all temporary artwork files created by [readTags].
     * Call after the artwork is no longer needed (e.g., after displaying or saving).
     */
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
}
