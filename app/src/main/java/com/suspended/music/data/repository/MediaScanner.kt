package com.suspended.music.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis
import com.suspended.music.data.preferences.SettingsManager

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val settingsManager: SettingsManager
) {
    
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
    
    suspend fun scanMedia(): ScanResult = withContext(Dispatchers.IO) {
        val scannedSongs = mutableListOf<Song>()
        val albumsMap = linkedMapOf<Long, Album>() // LinkedMap to maintain insertion order
        val artistsMap = linkedMapOf<String, Artist>()
        
        val scanStartTime = System.currentTimeMillis()
        
        try {
            // Load existing songs to preserve user data (favorites, play counts, etc.)
            val existingSongsMap = mutableMapOf<Long, Song>()
            songDao.getAllSongsSync().forEach { existingSongsMap[it.id] = it }
            
            Log.d(TAG, "Starting media scan with ${existingSongsMap.size} existing songs in database")

            // Query sorted by album first, then title for better performance with album loading
            contentResolver.query(
                audioUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.ALBUM_ID} ASC, ${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                
                val totalItems = cursor.count
                Log.d(TAG, "Found $totalItems audio files to scan")
                
                var itemsProcessed = 0
                while (cursor.moveToNext()) {
                    itemsProcessed++
                    
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)?.trim().takeIf { !it.isNullOrEmpty() } ?: "Unknown"
                    val artist = cursor.getString(artistColumn)?.trim().takeIf { !it.isNullOrEmpty() } ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn)?.trim().takeIf { !it.isNullOrEmpty() } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(pathColumn) ?: ""
                    val trackNumber = cursor.getInt(trackColumn)
                    val year = cursor.getInt(yearColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    val dateModified = cursor.getLong(dateModifiedColumn) * 1000
                    val size = cursor.getLong(sizeColumn)
                    
                    // Skip songs with invalid paths
                    if (path.isEmpty()) {
                        Log.w(TAG, "Skipping song with empty path: $title")
                        continue
                    }
                    
                    val contentUri = ContentUris.withAppendedId(audioUri, id)

                    // Preserve user data from existing song if it exists
                    val existing = existingSongsMap[id]
                    
                    val song = Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = duration,
                        path = path,
                        uri = contentUri.toString(),
                        trackNumber = trackNumber,
                        year = year,
                        genre = existing?.genre ?: "",
                        bitrate = existing?.bitrate ?: 0,
                        sampleRate = existing?.sampleRate ?: 0,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        isFavorite = existing?.isFavorite ?: false,
                        playCount = existing?.playCount ?: 0,
                        lastPlayed = existing?.lastPlayed ?: 0,
                        lyrics = existing?.lyrics
                    )
                    var finalSong = song
                    
                    
                    // Extract advanced metadata if missing (bitrate, sample rate)
                    if (finalSong.bitrate == 0 || finalSong.sampleRate == 0) {
                        val metadataTime = measureTimeMillis {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, contentUri)
                                
                                var newBitrate = finalSong.bitrate
                                if (newBitrate == 0) {
                                    val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                                    newBitrate = bitrateStr?.toIntOrNull()?.div(1000) ?: 0
                                }
                                
                                var newSampleRate = finalSong.sampleRate
                                if (newSampleRate == 0) {
                                    val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                                    newSampleRate = sampleRateStr?.toIntOrNull() ?: 0
                                }
                                
                                if (newBitrate != finalSong.bitrate || newSampleRate != finalSong.sampleRate) {
                                    finalSong = finalSong.copy(
                                        bitrate = if (newBitrate > 0) newBitrate else finalSong.bitrate,
                                        sampleRate = if (newSampleRate > 0) newSampleRate else finalSong.sampleRate
                                    )
                                }
                            } catch (e: Exception) {
                                // Ignore metadata extraction errors - use defaults
                                Log.d(TAG, "Could not extract metadata for $title: ${e.message}")
                            } finally {
                                try {
                                    retriever.release()
                                } catch (e: Exception) {
                                    // Ignore release errors
                                }
                            }
                        }
                        
                        // Log slow metadata extractions (over 500ms)
                        if (metadataTime > 500) {
                            Log.w(TAG, "Slow metadata extraction for $title: ${metadataTime}ms")
                        }
                    }
                    
                    scannedSongs.add(finalSong)
                    
                    // Collect albums
                    if (!albumsMap.containsKey(albumId)) {
                        albumsMap[albumId] = Album(
                            id = albumId,
                            title = album,
                            artist = artist,
                            year = year,
                            songCount = 0, // Will update later
                            dateAdded = dateAdded
                        )
                    }
                    
                    // Collect artists
                    if (!artistsMap.containsKey(artist)) {
                        artistsMap[artist] = Artist(
                            id = artist.hashCode().toLong(),
                            name = artist,
                            songCount = 0, // Will update later
                            albumCount = 0 // Will update later
                        )
                    }
                    
                    // Log progress every 25 songs
                    if (itemsProcessed % 25 == 0) {
                        Log.d(TAG, "Scan progress: $itemsProcessed/$totalItems songs processed")
                    }
                }
            }
            
            // Update album song counts
            val albumSongCounts = scannedSongs.groupBy { it.albumId }.mapValues { it.value.size }
            val albums = albumsMap.values.map { album ->
                album.copy(songCount = albumSongCounts[album.id] ?: 0)
            }
            
            // Update artist counts
            val artistSongCounts = scannedSongs.groupBy { it.artist }.mapValues { it.value.size }
            val artistAlbumCounts = scannedSongs.groupBy { it.artist }.mapValues { entry ->
                entry.value.map { it.albumId }.distinct().size
            }
            val artists = artistsMap.values.map { artist ->
                artist.copy(
                    songCount = artistSongCounts[artist.name] ?: 0,
                    albumCount = artistAlbumCounts[artist.name] ?: 0
                )
            }
            
            // Differential save: remove songs no longer on device, upsert current songs
            val scannedIds = scannedSongs.map { it.id }.toSet()
            val existingIds = existingSongsMap.keys
            val removedIds = existingIds - scannedIds
            val newSongs = scannedIds.count { !existingIds.contains(it) }
            
            Log.d(TAG, "Scan results: ${scannedSongs.size} songs, ${albums.size} albums, ${artists.size} artists")
            Log.d(TAG, "Changes: ${removedIds.size} songs removed, $newSongs new songs")
            
            // Delete songs that no longer exist on device
            removedIds.forEach { id -> songDao.deleteSongById(id) }
            
            // Upsert all scanned songs (INSERT OR REPLACE preserves data via our merge above)
            songDao.insertSongs(scannedSongs)
            
            // Albums and artists can be fully replaced (no user data to preserve)
            albumDao.deleteAllAlbums()
            artistDao.deleteAllArtists()
            albumDao.insertAlbums(albums)
            artistDao.insertArtists(artists)
            
            val scanDuration = System.currentTimeMillis() - scanStartTime
            Log.i(TAG, "✓ Scan completed in ${scanDuration}ms (~${if (scanDuration > 0) scannedSongs.size * 1000 / scanDuration else 0} songs/sec)")
            
            ScanResult.Success(
                songCount = scannedSongs.size,
                albumCount = albums.size,
                artistCount = artists.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "✗ Media scan failed: ${e.message}", e)
            ScanResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    companion object {
        private const val TAG = "MediaScanner"
    }
}

sealed class ScanResult {
    data class Success(
        val songCount: Int,
        val albumCount: Int,
        val artistCount: Int
    ) : ScanResult()
    
    data class Error(val message: String) : ScanResult()
}
