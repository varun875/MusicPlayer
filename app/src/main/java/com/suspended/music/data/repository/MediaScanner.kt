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
import androidx.room.withTransaction
import com.suspended.music.data.local.AlbumDao
import com.suspended.music.data.local.MusicDatabase
import com.suspended.music.data.local.ArtistDao
import com.suspended.music.data.local.SongDao
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.Artist
import com.suspended.music.data.model.Song
import com.suspended.music.data.preferences.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Scans the device's MediaStore for audio files and syncs the results with the
 * local Room database.
 *
 * Pipeline:
 *  1. Load    — read existing songs so user data (favorites, play counts) survives
 *  2. Query   — read MediaStore rows → Song objects
 *  3. Enrich  — extract bitrate/sampleRate via a small pool of reusable
 *               MediaMetadataRetrievers (only for songs missing that data)
 *  4. Aggregate — derive Album & Artist rows from the song list
 *  5. Persist — diff against the DB and apply only the changes, in one transaction
 */
@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val settingsManager: SettingsManager
) {

    // ── Scan progress ──────────────────────────────────────────────────

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    /** Serializes scans: a second call while one is running is rejected. */
    private val scanMutex = Mutex()

    // ── MediaStore config ──────────────────────────────────────────────

    private val contentResolver: ContentResolver = context.contentResolver

    private val audioUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    // MIME_TYPE is used in the WHERE clause but not read, so it stays out of
    // the projection. ARTIST_ID is read to give artists stable, collision-free ids.
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.COMPOSER
    )

    // IS_MUSIC is often 0 for files in Downloads, so also accept any audio/* MIME.
    // Additionally, some valid audio containers (M4A, AAC, Opus, OGG, WMA, ...) report
    // an empty/generic MIME type, so we also match on the file extension.
    private val selection = """
        (${MediaStore.Audio.Media.IS_MUSIC} != 0
         OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE ?
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.aac'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.m4a'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.m4b'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.mp4'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.opus'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.ogg'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.oga'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.wma'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.aiff'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.aif'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.dsf'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.dff'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.flac'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.mp3'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.wav'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.mka'
         OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.amr')
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
     * Full scan. Safe to call from any dispatcher — switches to [Dispatchers.IO].
     * Returns an error result (rather than queueing) if a scan is already running.
     */
    suspend fun scanMedia(): ScanResult {
        if (!scanMutex.tryLock()) {
            Log.w(TAG, "Scan already in progress; ignoring request")
            return ScanResult.Error("A scan is already in progress")
        }
        try {
            return runScan()
        } finally {
            scanMutex.unlock()
        }
    }

    private suspend fun runScan(): ScanResult = withContext(Dispatchers.IO) {
        val scanStart = System.currentTimeMillis()
        _progress.value = ScanProgress(phase = ScanPhase.QUERYING)

        try {
            // 1. Existing songs, keyed by id, to preserve user data.
            val existingSongs = loadExistingSongs()
            Log.d(TAG, "Loaded ${existingSongs.size} existing songs from database")

            // 2. Query & parse MediaStore.
            _progress.value = ScanProgress(phase = ScanPhase.SCANNING)
            val parsed = queryAndParseSongs(existingSongs)
            Log.d(TAG, "Scanned ${parsed.songs.size} songs from MediaStore")

            // 3. Enrich only songs missing bitrate/sampleRate.
            _progress.value = ScanProgress(phase = ScanPhase.ENRICHING)
            val enrichedSongs = enrichMetadata(parsed.songs)

            // 4. Derive aggregates.
            _progress.value = ScanProgress(phase = ScanPhase.AGGREGATING)
            val albums = buildAlbums(enrichedSongs, parsed.artistIds)
            val artists = buildArtists(enrichedSongs, parsed.artistIds)

            // 5. Diff + transactional persist.
            _progress.value = ScanProgress(phase = ScanPhase.PERSISTING)
            val diff = persistChanges(enrichedSongs, existingSongs, albums, artists)

            val elapsed = System.currentTimeMillis() - scanStart
            val throughput = if (elapsed > 0) enrichedSongs.size * 1000L / elapsed else 0
            Log.i(TAG, "✓ Scan completed in ${elapsed}ms (~$throughput songs/sec)")
            Log.d(TAG, "  ${diff.added} added, ${diff.updated} updated, " +
                    "${diff.removed} removed, ${diff.unchanged} unchanged")

            _progress.value = ScanProgress(phase = ScanPhase.DONE)
            ScanResult.Success(
                songCount = enrichedSongs.size,
                albumCount = albums.size,
                artistCount = artists.size
            )
        } catch (e: CancellationException) {
            // Cooperative cancellation must propagate, not be reported as a failure.
            _progress.value = ScanProgress(phase = ScanPhase.DONE)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "✗ Media scan failed: ${e.message}", e)
            _progress.value = ScanProgress(phase = ScanPhase.DONE)
            ScanResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    // ── Step 1: Load existing ──────────────────────────────────────────

    private suspend fun loadExistingSongs(): Map<Long, Song> =
        songDao.getAllSongsSync().associateBy { it.id }

    // ── Step 2: Query & parse ──────────────────────────────────────────

    private suspend fun queryAndParseSongs(existingSongs: Map<Long, Song>): ParsedMedia {
        val songs = mutableListOf<Song>()
        val artistIds = HashMap<String, Long>()

        // No ORDER BY: aggregation groups in memory and Room re-sorts on read,
        // so sorting here would only cost MediaStore a filesort over every row.
        contentResolver.query(audioUri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                val columns = CursorColumns(cursor)
                val total = cursor.count
                Log.d(TAG, "Found $total audio files to process")

                var processed = 0
                while (cursor.moveToNext()) {
                    processed++
                    parseSongFromCursor(cursor, columns, existingSongs, artistIds)
                        ?.let { songs.add(it) }

                    if (processed % 50 == 0) {
                        coroutineContext.ensureActive() // stay cancellable on long scans
                        _progress.value = ScanProgress(
                            phase = ScanPhase.SCANNING,
                            current = processed,
                            total = total
                        )
                    }
                }
            }

        return ParsedMedia(songs, artistIds)
    }

    /** Parses one cursor row, or returns null to skip it (e.g. empty path). */
    private fun parseSongFromCursor(
        cursor: Cursor,
        columns: CursorColumns,
        existingSongs: Map<Long, Song>,
        artistIds: MutableMap<String, Long>
    ): Song? {
        val id = cursor.getLong(columns.id)
        val path = cursor.getString(columns.path) ?: ""
        if (path.isEmpty()) {
            Log.w(TAG, "Skipping song with empty path (id=$id)")
            return null
        }

        val artist = cursor.getString(columns.artist)?.trim()
            .takeIf { !it.isNullOrEmpty() } ?: "Unknown Artist"
        // Record the canonical MediaStore artist id for aggregate building.
        artistIds[artist] = cursor.getLong(columns.artistId)

        val existing = existingSongs[id]
        return Song(
            id = id,
            title = cursor.getString(columns.title)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: "Unknown",
            artist = artist,
            album = cursor.getString(columns.album)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: "Unknown Album",
            albumId = cursor.getLong(columns.albumId),
            duration = cursor.getLong(columns.duration),
            path = path,
            uri = ContentUris.withAppendedId(audioUri, id).toString(),
            trackNumber = cursor.getInt(columns.track),
            year = cursor.getInt(columns.year),
            dateAdded = cursor.getLong(columns.dateAdded) * 1000,     // s → ms
            dateModified = cursor.getLong(columns.dateModified) * 1000,
            size = cursor.getLong(columns.size),
            // Preserve user data from the existing entry.
            genre = existing?.genre ?: "",
            composer = cursor.getString(columns.composer)?.trim()
                .takeIf { !it.isNullOrEmpty() } ?: existing?.composer ?: "",
            bitrate = existing?.bitrate ?: 0,
            sampleRate = existing?.sampleRate ?: 0,
            isFavorite = existing?.isFavorite ?: false,
            playCount = existing?.playCount ?: 0,
            lastPlayed = existing?.lastPlayed ?: 0,
            lyrics = existing?.lyrics
        )
    }

    // ── Step 3: Enrich metadata ────────────────────────────────────────

    /**
     * Fills in bitrate/sampleRate for songs missing them. A small pool of
     * reusable [MediaMetadataRetriever]s is shared across worker coroutines —
     * one retriever per worker, reused across that worker's whole chunk — so we
     * pay the retriever setup cost K times instead of once per song.
     */
    private suspend fun enrichMetadata(songs: List<Song>): List<Song> = coroutineScope {
        val needs = songs.filter { it.bitrate == 0 || it.sampleRate == 0 }
        val total = needs.size
        if (total == 0) return@coroutineScope songs

        Log.d(TAG, "$total/${songs.size} songs need metadata enrichment")
        val processed = AtomicInteger(0)
        val workerCount = ENRICH_CONCURRENCY.coerceIn(1, total)
        val chunks = needs.chunked((total + workerCount - 1) / workerCount)

        val deferred = chunks.map { chunk ->
            async {
                val retriever = MediaMetadataRetriever()
                try {
                    chunk.map { song ->
                        coroutineContext.ensureActive()
                        val updated = extractMetadata(song, retriever)
                        val done = processed.incrementAndGet()
                        if (done % 25 == 0 || done == total) {
                            _progress.value = ScanProgress(
                                phase = ScanPhase.ENRICHING,
                                current = done,
                                total = total
                            )
                        }
                        updated
                    }
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

        // Merge on the calling coroutine (single-threaded) — no shared mutable map.
        val enrichedById = HashMap<Long, Song>(total)
        for (list in deferred.awaitAll()) {
            for (song in list) enrichedById[song.id] = song
        }
        songs.map { enrichedById[it.id] ?: it }
    }

    /** Extracts bitrate/sample rate into [retriever]'s current data source. */
    private fun extractMetadata(song: Song, retriever: MediaMetadataRetriever): Song {
        return try {
            retriever.setDataSource(context, ContentUris.withAppendedId(audioUri, song.id))

            val bitrate = if (song.bitrate == 0) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()?.div(1000)?.coerceAtLeast(0) ?: 0
            } else song.bitrate

            val sampleRate = if (song.sampleRate == 0) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            } else song.sampleRate

            if (bitrate != song.bitrate || sampleRate != song.sampleRate) {
                song.copy(bitrate = bitrate, sampleRate = sampleRate)
            } else song
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Metadata extraction failed for '${song.title}': ${e.message}")
            song
        }
    }

    // ── Step 4: Aggregates ─────────────────────────────────────────────

    private fun buildAlbums(songs: List<Song>, artistIds: Map<String, Long>): List<Album> =
        songs
            // albumId 0 means "no album metadata"; grouping those would collapse
            // every unknown track into a single bogus album.
            .filter { it.albumId > 0 }
            .groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    artistId = stableArtistId(first.artist, artistIds),
                    year = albumSongs.maxOf { it.year },
                    songCount = albumSongs.size,
                    dateAdded = albumSongs.minOf { it.dateAdded }
                )
            }

    private fun buildArtists(songs: List<Song>, artistIds: Map<String, Long>): List<Artist> =
        songs.groupBy { it.artist }.map { (name, artistSongs) ->
            Artist(
                id = stableArtistId(name, artistIds),
                name = name,
                songCount = artistSongs.size,
                albumCount = artistSongs.distinctBy { it.albumId }.size
            )
        }

    /**
     * MediaStore's ARTIST_ID is 0/unknown for some tracks; using 0 for several
     * distinct artists makes REPLACE keep only one of them, so fall back to a
     * stable negative id that can't collide with real (positive) MediaStore ids.
     */
    private fun stableArtistId(name: String, artistIds: Map<String, Long>): Long {
        val mediaStoreId = artistIds[name]
        if (mediaStoreId != null && mediaStoreId > 0) return mediaStoreId
        return -(name.hashCode().toLong() and 0x7FFFFFFF) - 1
    }

    // ── Step 5: Persist ────────────────────────────────────────────────

    /**
     * Writes only what changed, in a single transaction. User fields were
     * preserved during parsing, so a scanned song compares unequal to its
     * existing row exactly when MediaStore data or enriched metadata changed —
     * letting us skip the (vast) unchanged majority on incremental scans.
     */
    private suspend fun persistChanges(
        scannedSongs: List<Song>,
        existingSongs: Map<Long, Song>,
        albums: List<Album>,
        artists: List<Artist>
    ): DiffResult {
        val scannedIds = HashSet<Long>(scannedSongs.size)
        for (song in scannedSongs) scannedIds.add(song.id)

        // Safety: a partially-available MediaStore (permission not yet effective,
        // unmounted volume, or a restrictive min-duration filter) must never be
        // interpreted as "the user deleted the whole library".
        val deletionLooksUnsafe = existingSongs.isNotEmpty() &&
            (scannedIds.isEmpty() || scannedIds.size * 10 < existingSongs.size)
        val removedIds = if (deletionLooksUnsafe) {
            Log.w(TAG, "Scan matched ${scannedIds.size} of ${existingSongs.size} existing songs — skipping removals")
            emptyList()
        } else {
            existingSongs.keys.filter { it !in scannedIds }
        }
        val toUpsert = scannedSongs.filter { existingSongs[it.id] != it }
        val added = toUpsert.count { it.id !in existingSongs }
        val updated = toUpsert.size - added

        Log.d(TAG, "Diff: ${scannedSongs.size} total, $added added, $updated updated, " +
                "${removedIds.size} removed")

        database.withTransaction {
            if (removedIds.isNotEmpty()) {
                removedIds.chunked(BATCH_SIZE).forEach { songDao.deleteSongsAndPlaylistEntries(it) }
            }
            if (toUpsert.isNotEmpty()) {
                toUpsert.chunked(BATCH_SIZE).forEach { songDao.insertSongs(it) }
            }
            // Albums & artists carry no user data — full replace within the txn.
            albumDao.deleteAllAlbums()
            artistDao.deleteAllArtists()
            albumDao.insertAlbums(albums)
            artistDao.insertArtists(artists)
        }

        return DiffResult(
            added = added,
            updated = updated,
            removed = removedIds.size,
            unchanged = scannedSongs.size - toUpsert.size
        )
    }

    // ── Cursor column cache ────────────────────────────────────────────

    /** Resolves column indices once per cursor, not once per row. */
    private class CursorColumns(cursor: Cursor) {
        val id: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val artistId: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val album: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumId: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val duration: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val path: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val track: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val year: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val dateAdded: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModified: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val size: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val composer: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
    }

    // ── Constants ──────────────────────────────────────────────────────

    companion object {
        private const val TAG = "MediaScanner"
        private const val BATCH_SIZE = 500

        /** Concurrent metadata extractors. I/O-bound; scale with cores, capped. */
        private val ENRICH_CONCURRENCY =
            Runtime.getRuntime().availableProcessors().coerceIn(4, 8)
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

private data class ParsedMedia(
    val songs: List<Song>,
    val artistIds: Map<String, Long>
)

private data class DiffResult(
    val added: Int,
    val updated: Int,
    val removed: Int,
    val unchanged: Int
)
