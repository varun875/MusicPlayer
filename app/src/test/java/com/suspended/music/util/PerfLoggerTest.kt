package com.suspended.music.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PerfLoggerTest {

    @Before
    fun setUp() {
        PerfLogger.clearTracesForTests()
    }

    @Test
    fun endTrace_returnsMinusOne_whenTraceWasNotStarted() {
        val duration = PerfLogger.endTrace("MissingTrace")
        assertEquals(-1.0, duration, 0.0)
    }
}
