package com.suspended.music.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An album in the music library.
 *
 * Persisted by Room (table `albums`) and used directly as a Compose UI model,
 * hence [@Immutable][Immutable]: every field is a `val` of a primitive or
 * `String` type, so Compose can treat instances as stable and skip
 * recomposition when the reference is unchanged.
 */
@Immutable
@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["artist"]),    // artist → albums lookups
        Index(value = ["dateAdded"])  // "recently added" sorting
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
    /**
     * MediaStore album-art content URI for this album.
     *
     * Resolved once at construction and cached instead of rebuilt on every
     * access: in a scrolling grid this is read many times per frame per card,
     * and a custom getter would allocate a fresh [String] (and pressure the
     * GC) on each read. As a body property it stays out of the data class's
     * generated equals/hashCode/copy, and [@Ignore][Ignore] keeps Room from
     * mapping the new backing field to a column.
     */
    @Ignore
    val albumArtUri: String = ALBUM_ART_BASE + id

    private companion object {
        // MediaStore.Audio.Albums album-art prefix: content://…/albumart/<id>
        const val ALBUM_ART_BASE = "content://media/external/audio/albumart/"
    }
}
