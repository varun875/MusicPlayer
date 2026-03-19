package com.suspended.music.data.repository

import org.junit.Test
import kotlin.system.measureTimeMillis

class MediaScannerMapBenchmarkTest {

    // Minimal mock for benchmark
    data class MockSong(val id: Long)

    @Test
    fun benchmarkMapCreation() {
        val songCount = 10_000
        val songs = (1..songCount).map { MockSong(it.toLong()) }

        // Warmup
        for (i in 1..10) {
            val map1 = mutableMapOf<Long, MockSong>()
            songs.forEach { map1[it.id] = it }

            val map2 = songs.associateBy { it.id }
        }

        val iterations = 100
        var totalTimeOriginal = 0L
        var totalTimeOptimized = 0L

        for (i in 1..iterations) {
            val time1 = measureTimeMillis {
                val existingSongsMap = mutableMapOf<Long, MockSong>()
                songs.forEach { existingSongsMap[it.id] = it }
            }
            totalTimeOriginal += time1

            val time2 = measureTimeMillis {
                val existingSongsMap = songs.associateBy { it.id }
            }
            totalTimeOptimized += time2
        }

        println("Original method (mutableMapOf + forEach): $totalTimeOriginal ms total over $iterations iterations")
        println("Optimized method (associateBy): $totalTimeOptimized ms total over $iterations iterations")
        println("Improvement: ${totalTimeOriginal - totalTimeOptimized} ms")
        if (totalTimeOriginal > 0) {
            println("Percentage improvement: ${(totalTimeOriginal - totalTimeOptimized) * 100 / totalTimeOriginal}%")
        }
    }
}
