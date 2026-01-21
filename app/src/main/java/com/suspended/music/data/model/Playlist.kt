package com.suspended.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a playlist in the music library
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val songCount: Int = 0,
    val duration: Long = 0, // Total duration in milliseconds
    val coverArtPath: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isSmartPlaylist: Boolean = false,
    val smartCriteria: String? = null // JSON criteria for smart playlists
)

/**
 * Junction table for playlist-song relationships
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val dateAdded: Long = System.currentTimeMillis()
)
