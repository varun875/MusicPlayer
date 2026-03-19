package com.suspended.music.data.repository

import org.junit.Test
import kotlin.random.Random

class MediaScannerOptimizationTest {

    data class MockSong(val artist: String, val albumId: Long)

    @Test
    fun benchmarkMediaScannerOptimization() {
        val artists = (1..50).map { "Artist $it" }

        // Generate a large dataset
        val scannedSongs = (1..100000).map {
            MockSong(
                artist = artists[Random.nextInt(artists.size)],
                albumId = Random.nextInt(500).toLong()
            )
        }

        // Warm up JIT
        for (i in 1..5) {
            runOriginal(scannedSongs)
            runOptimized(scannedSongs)
        }

        val originalTimes = mutableListOf<Long>()
        val optimizedTimes = mutableListOf<Long>()

        // Benchmark
        for (i in 1..10) {
            val t1 = System.nanoTime()
            runOriginal(scannedSongs)
            originalTimes.add(System.nanoTime() - t1)

            val t2 = System.nanoTime()
            runOptimized(scannedSongs)
            optimizedTimes.add(System.nanoTime() - t2)
        }

        val avgOriginal = originalTimes.average()
        val avgOptimized = optimizedTimes.average()

        println("Average original time (ms): ${avgOriginal / 1_000_000.0}")
        println("Average optimized time (ms): ${avgOptimized / 1_000_000.0}")
        println("Improvement: ${(avgOriginal - avgOptimized) / 1_000_000.0} ms (${((avgOriginal - avgOptimized) / avgOriginal) * 100}%)")
    }

    private fun runOriginal(scannedSongs: List<MockSong>) {
        val artistSongCounts = scannedSongs.groupBy { it.artist }.mapValues { it.value.size }
        val artistAlbumCounts = scannedSongs.groupBy { it.artist }.mapValues { entry ->
            entry.value.map { it.albumId }.distinct().size
        }
    }

    private fun runOptimized(scannedSongs: List<MockSong>) {
        val groupedByArtist = scannedSongs.groupBy { it.artist }
        val artistSongCounts = groupedByArtist.mapValues { it.value.size }
        val artistAlbumCounts = groupedByArtist.mapValues { entry ->
            entry.value.map { it.albumId }.distinct().size
        }
    }
}
