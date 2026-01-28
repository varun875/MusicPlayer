package com.suspended.music.util

import android.util.Log
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Centralized error handler for the Music Player app
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Logs an exception with additional context
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Handles playback errors specifically
     */
    fun handlePlaybackError(error: PlaybackException) {
        Log.e(TAG, "Playback error: ${error.message}", error)
        Log.e(TAG, "Error code: ${error.errorCodeName}")
        if (error.cause != null) {
            Log.e(TAG, "Root cause: ${error.cause?.message}")
        }
        
        // Could be extended to send to crash reporting service
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                Log.e(TAG, "File not found error - possibly removed from device")
            }
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                Log.e(TAG, "IO error - possibly network or storage issue")
            }
            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                Log.e(TAG, "Decoding failed - unsupported format or corrupted file")
            }
            else -> {
                Log.e(TAG, "Other playback error: ${error.errorCodeName}")
            }
        }
    }
    
    /**
     * Coroutine exception handler for background operations
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error: ${throwable.message}", throwable)
        // Could be extended to send to crash reporting service
    }
}