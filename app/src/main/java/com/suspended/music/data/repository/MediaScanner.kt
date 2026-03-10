package com.suspended.music.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.suspended.music.data.local.AlbumDao
import com.suspended.music.data.local.ArtistDao
import com.suspended.music.data.local.SongDao
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.Artist
import com.suspended.music.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.suspended.music.data.preferences.SettingsManager
import kotlin.coroutines.coroutineContext

/**
 * Scans the device's MediaStore for audio files and syncs the results
 * with the local Room database.
 *
 * Architecture:
 * ┌──────────┐     ┌───────────────┐     ┌──────────────┐
 * │MediaStore│ ──▶ │ MediaScanner  │ ──▶ │  Room DAOs   │
 * │ (source) │     │ (transform)   │     │ (persist)    │
 * └──────────┘     └───────────────┘     └──────────────┘
 *
 * The scan pipeline:
 * 1. Query — fetch rows from MediaStore
 * 2. Parse — convert cursor rows → Song objects, preserving user data
 * 3. Enrich — extract bitrate/sampleRate via MediaMetadataRetriever for new songs
 * 4. Aggregate — derive Album & Artist aggregates from song list
 * 5. Persist — diff against existing DB and batch-write changes
 */
@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val settingsManager: SettingsManager
) {

    // ── Scan Progress ──────────────────────────────────────────────────

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    // ── MediaStore Config ──────────────────────────────────────────────

    private val contentResolver: ContentResolver = context.contentResolver

    private val audioUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.MIME_TYPE
    )

    // IS_MUSIC is often 0 for files in the Downloads folder, so we also
    // accept any file whose MIME type starts with "audio/" as a fallback.
    private val selection = """
        (${MediaStore.Audio.Media.IS_MUSIC} != 0
         OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE ?)
        AND ${MediaStore.Audio.Media.DURATION} >= ?
        AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0
        AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0
        AND ${MediaStore.Audio.Media.IS_ALARM} = 0
    """.trimIndent()

    private val selectionArgs: Array<String>
        get() {
            val minDurationMs = settingsManager.settingsFlow.value.library.minTrackDuration * 1000
            return arrayOf("audio/%", minDurationMs.toString())
        }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Full scan: queries MediaStore, enriches metadata, diffs against DB, persists.
     * Safe to call from any dispatcher — internally switches to [Dispatchers.IO].
     */
    suspend fun scanMedia(): ScanResult = withContext(Dispatchers.IO) {
        val scanStart = System.currentTimeMillis()
        _progress.value = ScanProgress(phase = ScanPhase.QUERYING)

        try {
            // 1. Load existing songs to preserve user data (favorites, play counts, etc.)
            val existingSongs = loadExistingSongs()
            Log.d(TAG, "Loaded ${existingSongs.size} existing songs from database")

            // 2. Query & parse MediaStore
            _progress.value = ScanProgress(phase = ScanPhase.SCANNING)
            val scannedSongs = queryAndParseSongs(existingSongs)
            Log.d(TAG, "Scanned ${scannedSongs.size} songs from MediaStore")

            // 3. Enrich songs that are missing metadata (bitrate, sampleRate)
            _progress.value = ScanProgress(
                phase = ScanPhase.ENRICHING,
                total = scannedSongs.size
            )
            val enrichedSongs = enrichMetadata(scannedSongs)

            // 4. Derive album & artist aggregates
            _progress.value = ScanProgress(phase = ScanPhase.AGGREGATING)
            val albums = buildAlbums(enrichedSongs)
            val artists = buildArtists(enrichedSongs)

            // 5. Diff and persist to database
            _progress.value = ScanProgress(phase = ScanPhase.PERSISTING)
            val diff = persistChanges(enrichedSongs, existingSongs, albums, artists)

            val elapsed = System.currentTimeMillis() - scanStart
            val throughput = if (elapsed > 0) enrichedSongs.size * 1000 / elapsed else 0
            Log.i(TAG, "✓ Scan completed in ${elapsed}ms (~$throughput songs/sec)")
            Log.d(TAG, "  ${diff.added} added, ${diff.removed} removed, ${diff.unchanged} unchanged")

            _progress.value = ScanProgress(phase = ScanPhase.DONE)

            ScanResult.Success(
                songCount = enrichedSongs.size,
                albumCount = albums.size,
                artistCount = artists.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "✗ Media scan failed: ${e.message}", e)
            _progress.value = ScanProgress(phase = ScanPhase.DONE)
            ScanResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    // ── Step 1: Load Existing ──────────────────────────────────────────

    private suspend fun loadExistingSongs(): Map<Long, Song> {
        return songDao.getAllSongsSync().associateBy { it.id }
    }

    // ── Step 2: Query & Parse ──────────────────────────────────────────

    private fun queryAndParseSongs(existingSongs: Map<Long, Song>): List<Song> {
        val songs = mutableListOf<Song>()

        contentResolver.query(
            audioUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.ALBUM_ID} ASC, ${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val columns = CursorColumns(cursor)
            val total = cursor.count
            Log.d(TAG, "Found $total audio files to process")

            var processed = 0
            while (cursor.moveToNext()) {
                processed++
                parseSongFromCursor(cursor, columns, existingSongs)?.let { songs.add(it) }

                // Report progress every 50 songs
                if (processed % 50 == 0) {
                    _progress.value = ScanProgress(
                        phase = ScanPhase.SCANNING,
                        current = processed,
                        total = total
                    )
                }
            }
        }

        return songs
    }

    /**
     * Parses a single song from the cursor row.
     * Returns null if the row should be skipped (e.g. empty path).
     */
    private fun parseSongFromCursor(
        cursor: Cursor,
        columns: CursorColumns,
        existingSongs: Map<Long, Song>
    ): Song? {
        val id = cursor.getLong(columns.id)
        val path = cursor.getString(columns.path) ?: ""

        if (path.isEmpty()) {
            Log.w(TAG, "Skipping song with empty path (id=$id)")
            return null
        }

        val existing = existingSongs[id]
        val contentUri = ContentUris.withAppendedId(audioUri, id)

        return Song(
            id = id,
            title = cursor.getString(columns.title)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: "Unknown",
            artist = cursor.getString(columns.artist)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: "Unknown Artist",
            album = cursor.getString(columns.album)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: "Unknown Album",
            albumId = cursor.getLong(columns.albumId),
            duration = cursor.getLong(columns.duration),
            path = path,
            uri = contentUri.toString(),
            trackNumber = cursor.getInt(columns.track),
            year = cursor.getInt(columns.year),
            dateAdded = cursor.getLong(columns.dateAdded) * 1000,     // seconds → ms
            dateModified = cursor.getLong(columns.dateModified) * 1000,
            size = cursor.getLong(columns.size),
            // Preserve user data from existing entry
            genre = existing?.genre ?: "",
            bitrate = existing?.bitrate ?: 0,
            sampleRate = existing?.sampleRate ?: 0,
            isFavorite = existing?.isFavorite ?: false,
            playCount = existing?.playCount ?: 0,
            lastPlayed = existing?.lastPlayed ?: 0,
            lyrics = existing?.lyrics
        )
    }

    // ── Step 3: Enrich Metadata ────────────────────────────────────────

    /**
     * Extracts bitrate & sample rate via [MediaMetadataRetriever] for songs
     * that don't already have this data. This is the slowest part of the scan
     * because MMR opens each file individually.
     */
    private suspend fun enrichMetadata(songs: List<Song>): List<Song> {
        val totalToEnrich = songs.count { it.bitrate == 0 || it.sampleRate == 0 }
        if (totalToEnrich == 0) return songs

        Log.d(TAG, "$totalToEnrich/${songs.size} songs need metadata enrichment")

        val enriched = songs.toMutableList()
        var processed = 0

        for (index in enriched.indices) {
            coroutineContext.ensureActive()

            val song = enriched[index]
            if (song.bitrate != 0 && song.sampleRate != 0) continue

            processed++
            val updated = extractMetadata(song)
            if (updated !== song) {
                enriched[index] = updated
            }

            if (processed % 50 == 0 || processed == totalToEnrich) {
                _progress.value = ScanProgress(
                    phase = ScanPhase.ENRICHING,
                    current = processed,
                    total = totalToEnrich
                )
            }
        }

        return enriched
    }

    /**
     * Attempts to extract bitrate and sample rate for a single song.
     * Returns the original song if extraction fails.
     */
    private fun extractMetadata(song: Song): Song {
        val retriever = MediaMetadataRetriever()
        return try {
            val contentUri = ContentUris.withAppendedId(audioUri, song.id)
            retriever.setDataSource(context, contentUri)

            val bitrate = if (song.bitrate == 0) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()?.div(1000) ?: 0
            } else song.bitrate

            val sampleRate = if (song.sampleRate == 0) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull() ?: 0
            } else song.sampleRate

            if (bitrate != song.bitrate || sampleRate != song.sampleRate) {
                song.copy(
                    bitrate = bitrate.coerceAtLeast(0),
                    sampleRate = sampleRate.coerceAtLeast(0)
                )
            } else song
        } catch (e: Exception) {
            Log.d(TAG, "Metadata extraction failed for '${song.title}': ${e.message}")
            song
        } finally {
            runCatching { retriever.release() }
        }
    }

    // ── Step 4: Aggregates ─────────────────────────────────────────────

    /**
     * Derives album entries from the scanned song list, counting songs per album.
     */
    private fun buildAlbums(songs: List<Song>): List<Album> {
        val byAlbumId = songs.groupBy { it.albumId }
        return byAlbumId.map { (albumId, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                year = first.year,
                songCount = albumSongs.size,
                dateAdded = albumSongs.minOf { it.dateAdded }
            )
        }
    }

    /**
     * Derives artist entries from the scanned song list, counting songs & albums per artist.
     */
    private fun buildArtists(songs: List<Song>): List<Artist> {
        val byArtist = songs.groupBy { it.artist }
        return byArtist.map { (name, artistSongs) ->
            Artist(
                id = name.hashCode().toLong(),
                name = name,
                songCount = artistSongs.size,
                albumCount = artistSongs.map { it.albumId }.distinct().size
            )
        }
    }

    // ── Step 5: Persist ────────────────────────────────────────────────

    /**
     * Computes diff between scanned songs and existing DB, then applies
     * batched inserts/deletes for songs, and full-replaces albums & artists.
     */
    private suspend fun persistChanges(
        scannedSongs: List<Song>,
        existingSongs: Map<Long, Song>,
        albums: List<Album>,
        artists: List<Artist>
    ): DiffResult {
        val scannedIds = HashSet<Long>(scannedSongs.size)
        val existingIds = existingSongs.keys
        var addedCount = 0

        scannedSongs.forEach { song ->
            if (scannedIds.add(song.id) && song.id !in existingIds) {
                addedCount++
            }
        }

        val removedIds = (existingIds - scannedIds).toList()

        Log.d(TAG, "Diff: ${scannedSongs.size} total, $addedCount added, ${removedIds.size} removed")

        // Batch-delete removed songs
        if (removedIds.isNotEmpty()) {
            removedIds.chunked(BATCH_SIZE).forEach { batch ->
                songDao.deleteSongsByIds(batch)
            }
        }

        // Batch-upsert current songs (INSERT OR REPLACE)
        scannedSongs.chunked(BATCH_SIZE).forEach { batch ->
            songDao.insertSongs(batch)
        }

        // Albums & artists have no user data — full replace is fine
        albumDao.deleteAllAlbums()
        artistDao.deleteAllArtists()
        albumDao.insertAlbums(albums)
        artistDao.insertArtists(artists)

        return DiffResult(
            added = addedCount,
            removed = removedIds.size,
            unchanged = scannedSongs.size - addedCount
        )
    }

    // ── Cursor Column Cache ────────────────────────────────────────────

    /**
     * Pre-resolves column indices once per cursor to avoid repeated
     * [Cursor.getColumnIndexOrThrow] calls inside the row loop.
     */
    private class CursorColumns(cursor: Cursor) {
        val id: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val album: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumId: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val duration: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val path: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val track: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val year: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val dateAdded: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModified: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val size: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
    }

    // ── Constants ───────────────────────────────────────────────────────

    companion object {
        private const val TAG = "MediaScanner"
        private const val BATCH_SIZE = 500
    }
}

// ── Models ─────────────────────────────────────────────────────────────

sealed class ScanResult {
    data class Success(
        val songCount: Int,
        val albumCount: Int,
        val artistCount: Int
    ) : ScanResult()

    data class Error(val message: String) : ScanResult()
}

enum class ScanPhase {
    QUERYING,    // Loading existing data from DB
    SCANNING,    // Reading MediaStore cursor
    ENRICHING,   // Extracting bitrate/sampleRate via MMR
    AGGREGATING, // Building album & artist aggregates
    PERSISTING,  // Writing changes to DB
    DONE         // Scan finished
}

data class ScanProgress(
    val phase: ScanPhase = ScanPhase.DONE,
    val current: Int = 0,
    val total: Int = 0
) {
    val fraction: Float
        get() = if (total > 0) current.toFloat() / total else 0f
}

private data class DiffResult(
    val added: Int,
    val removed: Int,
    val unchanged: Int
)
