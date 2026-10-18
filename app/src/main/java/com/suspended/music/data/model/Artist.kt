package com.suspended.music.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an artist in the music library
 */
@Immutable
@Entity(
    tableName = "artists",
    indices = [
        Index(value = ["name"]) // For sorting/searching by name
    ]
)
data class Artist(
    @PrimaryKey
    val id: Long,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0
)
