package com.suspended.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.Artist
import com.suspended.music.data.model.Folder
import com.suspended.music.data.model.Playlist
import com.suspended.music.data.model.PlaylistSong
import com.suspended.music.data.model.Song

@Database(
    entities = [
        Song::class,
        Album::class,
        Artist::class,
        Playlist::class,
        PlaylistSong::class,
        Folder::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao
    
    companion object {
        const val DATABASE_NAME = "music_database"
    }
}
