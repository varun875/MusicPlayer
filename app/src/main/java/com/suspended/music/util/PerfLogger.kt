package com.suspended.music.util

import android.os.SystemClock
import android.util.Log

/**
 * Lightweight performance logger for measuring durations and logging events.
 * Uses monotonic clock (elapsedRealtimeNanos) for accurate timing.
 *
 * Usage:
 *   PerfLogger.startTrace("MyOperation")
 *   // ... do work ...
 *   PerfLogger.endTrace("MyOperation")  // logs duration
 *
 *   PerfLogger.logEvent("ScrollPerf", "items_visible=8")
 */
object PerfLogger {

    @PublishedApi internal const val TAG = "PulsePerf"

    // Active trace start times (thread-safe via ConcurrentHashMap)
    private val traces = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Start a named trace. Call [endTrace] with the same name to log the duration.
     */
    fun startTrace(name: String) {
        traces[name] = SystemClock.elapsedRealtimeNanos()
    }

    /**
     * End a named trace and log its duration in milliseconds.
     * Returns the duration in ms, or -1 if the trace was never started.
     */
    fun endTrace(name: String): Double {
        val endNanos = SystemClock.elapsedRealtimeNanos()
        val startNanos = traces.remove(name) ?: run {
            Log.w(TAG, "endTrace('$name') called but no matching startTrace found")
            return -1.0
        }
        val durationMs = (endNanos - startNanos) / 1_000_000.0
        Log.d(TAG, "⏱ $name: ${String.format("%.2f", durationMs)}ms")
        return durationMs
    }

    /**
     * Log a one-off performance event with optional details.
     */
    fun logEvent(event: String, details: String = "") {
        if (details.isNotEmpty()) {
            Log.d(TAG, "📊 $event: $details")
        } else {
            Log.d(TAG, "📊 $event")
        }
    }

    /**
     * Measure a block inline and log its duration. Returns the block's result.
     *
     * Usage:
     *   val sorted = PerfLogger.measure("SortSongs") { songs.sortedBy { it.title } }
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        val startNanos = SystemClock.elapsedRealtimeNanos()
        val result = block()
        val durationMs = (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000.0
        Log.d(TAG, "⏱ $name: ${String.format("%.2f", durationMs)}ms")
        return result
    }
}
