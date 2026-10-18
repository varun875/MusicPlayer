package com.suspended.music.util

import android.os.SystemClock
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight performance logger for measuring durations and logging events.
 * Timing uses the monotonic [SystemClock.elapsedRealtimeNanos] clock — immune
 * to wall-clock changes and keeps ticking through deep sleep.
 *
 * Every entry point is gated by [enabled], so the logger costs nothing when
 * switched off. Disable it in release builds (e.g. in Application.onCreate):
 *
 *     PerfLogger.enabled = BuildConfig.DEBUG
 *
 * Usage:
 *     PerfLogger.startTrace("MyOperation")
 *     // ... work ...
 *     PerfLogger.endTrace("MyOperation")          // logs + returns ms
 *
 *     val sorted = PerfLogger.measure("SortSongs") { songs.sortedBy { it.title } }
 *     val page   = PerfLogger.measureSuspend("LoadPage") { repo.loadPage() }
 *
 *     PerfLogger.logEvent("ScrollPerf", "items_visible=8")
 */
object PerfLogger {

    private const val TAG = "PulsePerf"
    private const val NANOS_PER_MS = 1_000_000.0

    /**
     * Master switch. When false, every method is a no-op — except
     * [measure]/[measureSuspend], which still run their block and return its
     * result, only skipping timing and logging. Defaults to on so current
     * debug logging is unchanged; set to `BuildConfig.DEBUG` for release.
     */
    @Volatile
    var enabled: Boolean = true

    /** Active trace start times, keyed by trace name. */
    private val traces = ConcurrentHashMap<String, Long>()

    internal fun clearTracesForTests() {
        traces.clear()
    }

    // ── Traces ──────────────────────────────────────────────────────────

    /** Start a named trace. Pair with [endTrace] using the same name. */
    fun startTrace(name: String) {
        if (!enabled) return
        traces[name] = nowNanos()
    }

    /**
     * End a named trace, log its duration, and return it in milliseconds.
     * Returns -1.0 if the trace was never started or logging is disabled.
     */
    fun endTrace(name: String): Double {
        if (!enabled) return -1.0
        val startNanos = traces.remove(name) ?: run {
            Log.w(TAG, "endTrace('$name') called but no matching startTrace found")
            return -1.0
        }
        val durationMs = (nowNanos() - startNanos) / NANOS_PER_MS
        Log.d(TAG, "⏱ $name: ${formatMs(durationMs)}ms")
        return durationMs
    }

    // ── Events ──────────────────────────────────────────────────────────

    /** Log a one-off performance event with optional details. */
    fun logEvent(event: String, details: String = "") {
        if (!enabled) return
        if (details.isEmpty()) Log.d(TAG, "📊 $event")
        else Log.d(TAG, "📊 $event: $details")
    }

    // ── Inline measurement ──────────────────────────────────────────────

    /**
     * Measure [block] and log its duration, returning the block's result.
     * Inline, so no closure is allocated at the call site.
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        if (!enabled) return block()
        val startNanos = nowNanos()
        val result = block()
        logMeasured(name, nowNanos() - startNanos)
        return result
    }

    /** Suspending variant of [measure] for coroutine work. */
    suspend inline fun <T> measureSuspend(
        name: String,
        crossinline block: suspend () -> T
    ): T {
        if (!enabled) return block()
        val startNanos = nowNanos()
        val result = block()
        logMeasured(name, nowNanos() - startNanos)
        return result
    }

    // ── Internals (@PublishedApi so the inline functions can call them) ──

    @PublishedApi
    internal fun nowNanos(): Long = SystemClock.elapsedRealtimeNanos()

    @PublishedApi
    internal fun logMeasured(name: String, elapsedNanos: Long) {
        val durationMs = elapsedNanos / NANOS_PER_MS
        Log.d(TAG, "⏱ $name: ${formatMs(durationMs)}ms")
    }

    private fun formatMs(ms: Double): String = String.format(Locale.US, "%.2f", ms)
}
