#!/bin/bash
# Create a simple test to measure N+1 vs batch deletes
cat << 'KOTLIN_EOF' > app/src/test/java/com/suspended/music/PerfTest.kt
package com.suspended.music

import org.junit.Test
import kotlin.system.measureTimeMillis

class PerfTest {
    @Test
    fun testPerformance() {
        println("Performance test running...")
    }
}
KOTLIN_EOF
./gradlew testDebugUnitTest --tests "com.suspended.music.PerfTest"
