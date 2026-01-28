package com.suspended.music.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a song/track in the music library
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["albumId"]), // For album queries
        Index(value = ["artist"]), // For artist queries
        Index(value = ["album"]), // For album name queries
        Index(value = ["genre"]), // For genre queries
        Index(value = ["isFavorite"]), // For favorites queries
        Index(value = ["dateAdded"]), // For recently added queries
        Index(value = ["lastPlayed"]), // For recently played queries
        Index(value = ["playCount"]), // For most played queries
        Index(value = ["title"]), // For title searches
        Index(value = ["duration"]) // For duration filtering
    ]
)
data class Song(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long, // in milliseconds
    val path: String,
    val uri: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val bitrate: Int = 0, // in kbps
    val sampleRate: Int = 0, // in Hz
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val size: Long = 0, // in bytes
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val lyrics: String? = null
) {
    val albumArtUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")
    
    val contentUri: Uri
        get() = Uri.parse(uri)
    
    val formattedDuration: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    
    val formattedBitrate: String
        get() = if (bitrate > 0) "${bitrate}kbps" else "Unknown"
    
    val formattedSampleRate: String
        get() = when {
            sampleRate >= 1000 -> "${sampleRate / 1000}kHz"
            sampleRate > 0 -> "${sampleRate}Hz"
            else -> "Unknown"
        }
    
    val isHiRes: Boolean
        get() = sampleRate > 48000 || bitrate > 320
}
