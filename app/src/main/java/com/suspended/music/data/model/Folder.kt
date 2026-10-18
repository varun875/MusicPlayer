package com.suspended.music.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a folder containing music files
 */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey
    val path: String,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val songCount: Int = 0,
    val parentPath: String? = null
)
