package com.suspended.music.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an album in the music library
 */
@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["artist"]), // For artist-album queries
        Index(value = ["dateAdded"]) // For sorting by date
    ]
)
data class Album(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val songCount: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val albumArtUri: String
        get() = "content://media/external/audio/albumart/$id"
}
