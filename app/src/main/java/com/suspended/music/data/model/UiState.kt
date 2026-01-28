package com.suspended.music.data.model

/**
 * Sealed interface for UI state management (Loading/Success/Error)
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : UiState<Nothing>
}

/**
 * Inline classes for type-safe ID handling
 */
@JvmInline
value class SongId(val value: Long)

@JvmInline
value class PlaylistId(val value: Long)

@JvmInline
value class AlbumId(val value: String)

@JvmInline
value class ArtistId(val value: String)
