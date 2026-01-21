package com.suspended.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suspended.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongsByDateAdded(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>
    
    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 50): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?
    
    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE path LIKE :folderPath || '%' ORDER BY title ASC")
    fun getSongsByFolder(folderPath: String): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<Song>>
    
    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>
    
    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<Song>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
    
    @Update
    suspend fun updateSong(song: Song)
    
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteSong(song: Song)
    
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)
    
    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
    
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
