package com.suspended.music.data.model

import android.net.Uri

data class AudioTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val composer: String = "",
    val comment: String = "",
    val albumArtist: String = "",
    val lyrics: String = "",
    val artworkUri: Uri? = null,
    val hasArtworkChanged: Boolean = false
)
