package com.suspended.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suspended.music.data.model.Artist
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<Artist>>
    
    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: Long): Artist?
    
    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchArtists(query: String): Flow<List<Artist>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<Artist>)
    
    @Delete
    suspend fun deleteArtist(artist: Artist)
    
    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()
    
    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int
}
