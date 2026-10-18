package com.suspended.music.util

import android.util.Log
import androidx.media3.common.PlaybackException
import com.suspended.music.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Centralized error reporting for the music player.
 *
 * Everything funnels through [report], which is deliberately the *only* place
 * an error is emitted — so adding a crash-reporting SDK later is a one-line
 * change, not a hunt through scattered Log calls.
 *
 * This object is a reporter, not a responder: it records what went wrong. The
 * *reaction* to a playback error (skip the track, delete a dead file, retry)
 * belongs in the player, keyed off [PlaybackException.errorCode].
 */
object ErrorHandler {

    private const val TAG = "ErrorHandler"

    /**
     * The single reporting funnel. Logs now; add crash reporting here once.
     */
    private fun report(tag: String, message: String, throwable: Throwable? = null) {
        // In release, log only the exception *type* — exception messages and stack
        // traces routinely embed user file paths and track titles.
        if (!BuildConfig.DEBUG) {
            val suffix = throwable?.let { " [${it.javaClass.simpleName}]" } ?: ""
            Log.e(tag, message + suffix)
            return
        }
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        // TODO(crash-reporting): e.g. Crashlytics.recordException / Sentry.captureException
    }

    /** Log an error with optional context. The canonical public entry point. */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        report(tag, message, throwable)
    }

    /**
     * Report a playback error as a single, self-contained entry. The throwable
     * is passed through (debug builds log the full stack) but the message itself
     * stays free of the media path.
     */
    fun handlePlaybackError(error: PlaybackException) {
        report(TAG, playbackErrorText(error), error)
    }

    /**
     * User-facing, PII-free description of a playback failure. Deliberately omits
     * [PlaybackException.message], which embeds the media URI / file path.
     */
    fun playbackErrorText(error: PlaybackException): String = buildString {
        append("Playback error: ").append(error.errorCodeName)
        playbackHint(error.errorCode)?.let { append(" — ").append(it) }
    }

    /** Short human-readable hint for the common, actionable error codes. */
    private fun playbackHint(code: Int): String? = when (code) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "file missing, possibly removed from device"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
            "I/O problem, possibly network or storage"
        PlaybackException.ERROR_CODE_DECODING_FAILED ->
            "unsupported or corrupted media"
        else -> null
    }

    /**
     * Last-resort net for uncaught coroutine exceptions. Note this only fires
     * for failures that escape a `launch` with no parent to propagate to — it
     * never sees `CancellationException`, and `async` failures surface at
     * `await`, not here. It supplements, not replaces, handling errors at source.
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        report(TAG, "Uncaught coroutine exception", throwable)
    }
}
