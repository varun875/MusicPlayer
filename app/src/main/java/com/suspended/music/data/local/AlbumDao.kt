package com.suspended.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suspended.music.data.model.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    
    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAllAlbums(): Flow<List<Album>>
    
    @Query("SELECT * FROM albums ORDER BY dateAdded DESC")
    fun getAlbumsByDateAdded(): Flow<List<Album>>
    
    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbumById(id: Long): Flow<Album?>
    
    @Query("SELECT * FROM albums WHERE artist = :artistName ORDER BY year DESC")
    fun getAlbumsByArtist(artistName: String): Flow<List<Album>>
    
    @Query("SELECT * FROM albums WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchAlbums(query: String): Flow<List<Album>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<Album>)
    
    @Delete
    suspend fun deleteAlbum(album: Album)
    
    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()
    
    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int
}
