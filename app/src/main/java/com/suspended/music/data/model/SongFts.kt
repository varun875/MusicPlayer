package com.suspended.music.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table mapping to [Song] for optimized full-text searching.
 */
@Fts4(contentEntity = Song::class)
@Entity(tableName = "songs_fts")
data class SongFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    val title: String,
    val artist: String,
    val album: String,
    val composer: String
)
