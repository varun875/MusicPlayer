package com.suspended.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.suspended.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY title ASC")
    @RewriteQueriesToDropUnusedColumns
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongsSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByArtistSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByAlbumSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY dateAdded DESC, id DESC")
    suspend fun getAllSongsByDateAddedSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY duration DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByDurationSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsPaged(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getAllSongsPagedByArtist(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getAllSongsPagedByAlbum(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY dateAdded DESC, id DESC")
    fun getAllSongsPagedByDateAdded(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY duration DESC, title COLLATE NOCASE ASC")
    fun getAllSongsPagedByDuration(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY playCount DESC, lastPlayed DESC, title COLLATE NOCASE ASC")
    fun getAllSongsPagedByPlayCount(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY size DESC, title COLLATE NOCASE ASC")
    fun getAllSongsPagedBySize(): androidx.paging.PagingSource<Int, Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY dateModified DESC, id DESC")
    fun getAllSongsPagedByDateModified(): androidx.paging.PagingSource<Int, Song>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY playCount DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByPlayCountSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY size DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsBySizeSync(): List<Song>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY dateModified DESC, id DESC")
    suspend fun getAllSongsByDateModifiedSync(): List<Song>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY dateAdded DESC")
    fun getAllSongsByDateAdded(): Flow<List<Song>>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 50): Flow<List<Song>>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<Song>>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    suspend fun getFavoritesSync(): List<Song>
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC")
    suspend fun getSongsByAlbumSync(albumId: Long): List<Song>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    suspend fun getSongsByArtistSync(artist: String): List<Song>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE path LIKE :folderPath || '%' ORDER BY title ASC")
    fun getSongsByFolder(folderPath: String): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' ESCAPE '\\' OR artist LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY title ASC")
    @RewriteQueriesToDropUnusedColumns
    fun searchSongs(query: String): Flow<List<Song>>

    @Query("""
        SELECT s.* FROM songs s
        JOIN songs_fts f ON s.id = f.rowid
        WHERE songs_fts MATCH :sanitizedQuery
        ORDER BY s.title ASC
    """)
    @RewriteQueriesToDropUnusedColumns
    fun searchSongsFts(sanitizedQuery: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' ESCAPE '\\' OR artist LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY title ASC")
    fun searchSongsPaged(query: String): androidx.paging.PagingSource<Int, Song>

    @Query("""
        SELECT s.* FROM songs s
        JOIN songs_fts f ON s.id = f.rowid
        WHERE songs_fts MATCH :sanitizedQuery
        ORDER BY s.title ASC
    """)
    fun searchSongsFtsPaged(sanitizedQuery: String): androidx.paging.PagingSource<Int, Song>
    
    @Query("""
        SELECT * FROM songs
        WHERE (:query IS NULL OR :query = '' OR title LIKE '%' || :query || '%' ESCAPE '\' OR artist LIKE '%' || :query || '%' ESCAPE '\' OR album LIKE '%' || :query || '%' ESCAPE '\' OR composer LIKE '%' || :query || '%' ESCAPE '\')
        AND (:artist IS NULL OR artist = :artist)
        AND (:album IS NULL OR album = :album)
        AND (:genre IS NULL OR genre = :genre)
        AND (:composer IS NULL OR composer LIKE '%' || :composer || '%' ESCAPE '\')
        AND (:folder IS NULL OR path LIKE :folder || '%' ESCAPE '\')
        AND (:lyrics IS NULL OR lyrics LIKE '%' || :lyrics || '%' ESCAPE '\')
        AND (:minDuration IS NULL OR duration >= :minDuration)
        AND (:maxDuration IS NULL OR duration <= :maxDuration)
        AND (:minYear IS NULL OR year >= :minYear)
        AND (:maxYear IS NULL OR year <= :maxYear)
        AND (:favoritesOnly = 0 OR isFavorite = 1)
        AND (:hiRes = 0 OR sampleRate > 48000 OR bitrate > 320)
        ORDER BY title ASC
    """)
    fun searchSongsWithFilters(
        query: String,
        artist: String?,
        album: String?,
        genre: String?,
        composer: String?,
        folder: String?,
        lyrics: String?,
        minDuration: Long?,
        maxDuration: Long?,
        minYear: Int?,
        maxYear: Int?,
        favoritesOnly: Int,
        hiRes: Int
    ): Flow<List<Song>>

    @Query("""
        SELECT s.* FROM songs s
        JOIN songs_fts f ON s.id = f.rowid
        WHERE songs_fts MATCH :sanitizedQuery
        AND (:artist IS NULL OR s.artist = :artist)
        AND (:album IS NULL OR s.album = :album)
        AND (:genre IS NULL OR s.genre = :genre)
        AND (:composer IS NULL OR s.composer LIKE '%' || :composer || '%' ESCAPE '\')
        AND (:folder IS NULL OR s.path LIKE :folder || '%' ESCAPE '\')
        AND (:lyrics IS NULL OR s.lyrics LIKE '%' || :lyrics || '%' ESCAPE '\')
        AND (:minDuration IS NULL OR s.duration >= :minDuration)
        AND (:maxDuration IS NULL OR s.duration <= :maxDuration)
        AND (:minYear IS NULL OR s.year >= :minYear)
        AND (:maxYear IS NULL OR s.year <= :maxYear)
        AND (:favoritesOnly = 0 OR s.isFavorite = 1)
        AND (:hiRes = 0 OR s.sampleRate > 48000 OR s.bitrate > 320)
        ORDER BY s.title ASC
    """)
    fun searchSongsWithFiltersFts(
        sanitizedQuery: String,
        artist: String?,
        album: String?,
        genre: String?,
        composer: String?,
        folder: String?,
        lyrics: String?,
        minDuration: Long?,
        maxDuration: Long?,
        minYear: Int?,
        maxYear: Int?,
        favoritesOnly: Int,
        hiRes: Int
    ): Flow<List<Song>>

    @Query("""
        SELECT * FROM songs
        WHERE (:query IS NULL OR :query = '' OR title LIKE '%' || :query || '%' ESCAPE '\' OR artist LIKE '%' || :query || '%' ESCAPE '\' OR album LIKE '%' || :query || '%' ESCAPE '\' OR composer LIKE '%' || :query || '%' ESCAPE '\')
        AND (:artist IS NULL OR artist = :artist)
        AND (:album IS NULL OR album = :album)
        AND (:genre IS NULL OR genre = :genre)
        AND (:composer IS NULL OR composer LIKE '%' || :composer || '%' ESCAPE '\')
        AND (:folder IS NULL OR path LIKE :folder || '%' ESCAPE '\')
        AND (:lyrics IS NULL OR lyrics LIKE '%' || :lyrics || '%' ESCAPE '\')
        AND (:minDuration IS NULL OR duration >= :minDuration)
        AND (:maxDuration IS NULL OR duration <= :maxDuration)
        AND (:minYear IS NULL OR year >= :minYear)
        AND (:maxYear IS NULL OR year <= :maxYear)
        AND (:favoritesOnly = 0 OR isFavorite = 1)
        AND (:hiRes = 0 OR sampleRate > 48000 OR bitrate > 320)
        ORDER BY title ASC
    """)
    fun searchSongsWithFiltersPaged(
        query: String,
        artist: String?,
        album: String?,
        genre: String?,
        composer: String?,
        folder: String?,
        lyrics: String?,
        minDuration: Long?,
        maxDuration: Long?,
        minYear: Int?,
        maxYear: Int?,
        favoritesOnly: Int,
        hiRes: Int
    ): androidx.paging.PagingSource<Int, Song>

    @Query("""
        SELECT s.* FROM songs s
        JOIN songs_fts f ON s.id = f.rowid
        WHERE songs_fts MATCH :sanitizedQuery
        AND (:artist IS NULL OR s.artist = :artist)
        AND (:album IS NULL OR s.album = :album)
        AND (:genre IS NULL OR s.genre = :genre)
        AND (:composer IS NULL OR s.composer LIKE '%' || :composer || '%' ESCAPE '\')
        AND (:folder IS NULL OR s.path LIKE :folder || '%' ESCAPE '\')
        AND (:lyrics IS NULL OR s.lyrics LIKE '%' || :lyrics || '%' ESCAPE '\')
        AND (:minDuration IS NULL OR s.duration >= :minDuration)
        AND (:maxDuration IS NULL OR s.duration <= :maxDuration)
        AND (:minYear IS NULL OR s.year >= :minYear)
        AND (:maxYear IS NULL OR s.year <= :maxYear)
        AND (:favoritesOnly = 0 OR s.isFavorite = 1)
        AND (:hiRes = 0 OR s.sampleRate > 48000 OR s.bitrate > 320)
        ORDER BY s.title ASC
    """)
    fun searchSongsWithFiltersFtsPaged(
        sanitizedQuery: String,
        artist: String?,
        album: String?,
        genre: String?,
        composer: String?,
        folder: String?,
        lyrics: String?,
        minDuration: Long?,
        maxDuration: Long?,
        minYear: Int?,
        maxYear: Int?,
        favoritesOnly: Int,
        hiRes: Int
    ): androidx.paging.PagingSource<Int, Song>

    @Query("""
        SELECT * FROM songs
        WHERE lyrics IS NOT NULL AND lyrics != ''
        ORDER BY title ASC
    """)
    fun searchSongsByLyrics(): Flow<List<Song>>
    
    @Query("""
        SELECT DISTINCT composer FROM songs WHERE composer != '' ORDER BY composer ASC LIMIT 100
    """)
    fun getAllComposerNames(): Flow<List<String>>
    
    @Query("""
        SELECT DISTINCT rtrim(path, replace(path, '/', '')) AS folder
        FROM songs
        WHERE path != '' AND instr(path, '/') > 0
        ORDER BY folder ASC
    """)
    fun getAllFolderPaths(): Flow<List<String>>
    
    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>
    
    @Query("SELECT DISTINCT artist FROM songs WHERE artist != '' ORDER BY artist ASC LIMIT 100")
    fun getAllArtistNames(): Flow<List<String>>
    
    @Query("SELECT DISTINCT album FROM songs WHERE album != '' ORDER BY album ASC LIMIT 100")
    fun getAllAlbumNames(): Flow<List<String>>
    
    @Query("SELECT id, title, artist, album, albumId, duration, path, uri, trackNumber, year, genre, composer, bitrate, sampleRate, dateAdded, dateModified, size, isFavorite, playCount, lastPlayed FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<Song>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    suspend fun insertSongs(songs: List<Song>)
    
    @Update
    @Transaction
    suspend fun updateSong(song: Song)
    
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    /** Atomic toggle — avoids the read-modify-write race from a stale UI model. */
    @Query("UPDATE songs SET isFavorite = 1 - isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)
    
    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteSong(song: Song)
    
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun deletePlaylistEntriesForSong(songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId IN (:songIds)")
    suspend fun deletePlaylistEntriesForSongs(songIds: List<Long>)

    /** Playlist ids that contain a song — captured before its entries are removed. */
    @Query("SELECT DISTINCT playlistId FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistIdsContainingSong(songId: Long): List<Long>

    @Query("SELECT DISTINCT playlistId FROM playlist_songs WHERE songId IN (:songIds)")
    suspend fun getPlaylistIdsContainingSongs(songIds: List<Long>): List<Long>

    /** Recomputes denormalized playlist stats from the junction table. */
    @Query("""
        UPDATE playlists SET
          songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId),
          duration = (
            SELECT COALESCE(SUM(s.duration), 0) FROM songs s
            INNER JOIN playlist_songs ps ON s.id = ps.songId
            WHERE ps.playlistId = :playlistId
          ),
          dateModified = :timestamp
        WHERE id = :playlistId
    """)
    suspend fun refreshPlaylistStats(playlistId: Long, timestamp: Long)

    /**
     * Deletes a song and its playlist membership together, so no orphans remain,
     * then refreshes the denormalized stats of every affected playlist —
     * otherwise their songCount/duration stay stale forever.
     */
    @Transaction
    suspend fun deleteSongAndPlaylistEntries(id: Long) {
        val affectedPlaylists = getPlaylistIdsContainingSong(id)
        deletePlaylistEntriesForSong(id)
        deleteSongById(id)
        val now = System.currentTimeMillis()
        affectedPlaylists.forEach { refreshPlaylistStats(it, now) }
    }

    @Transaction
    suspend fun deleteSongsAndPlaylistEntries(ids: List<Long>) {
        val affectedPlaylists = getPlaylistIdsContainingSongs(ids)
        deletePlaylistEntriesForSongs(ids)
        deleteSongsByIds(ids)
        val now = System.currentTimeMillis()
        affectedPlaylists.forEach { refreshPlaylistStats(it, now) }
    }

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
    
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT COUNT(*) FROM songs")
    fun observeSongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>
}
