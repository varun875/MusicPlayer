import java.util.Random

data class Song(val artist: String, val albumId: Long)

fun main() {
    val random = Random()
    val artists = listOf("Artist A", "Artist B", "Artist C", "Artist D", "Artist E", "Artist F", "Artist G", "Artist H", "Artist I", "Artist J")

    val scannedSongs = (1..100000).map {
        Song(
            artist = artists[random.nextInt(artists.size)],
            albumId = random.nextInt(500).toLong()
        )
    }

    val start1 = System.nanoTime()
    // Original implementation
    val artistSongCounts1 = scannedSongs.groupBy { it.artist }.mapValues { it.value.size }
    val artistAlbumCounts1 = scannedSongs.groupBy { it.artist }.mapValues { entry ->
        entry.value.map { it.albumId }.distinct().size
    }
    val time1 = System.nanoTime() - start1

    val start2 = System.nanoTime()
    // Optimized implementation
    val groupedByArtist = scannedSongs.groupBy { it.artist }
    val artistSongCounts2 = groupedByArtist.mapValues { it.value.size }
    val artistAlbumCounts2 = groupedByArtist.mapValues { entry ->
        entry.value.map { it.albumId }.distinct().size
    }
    val time2 = System.nanoTime() - start2

    println("Original time (ns): \$time1")
    println("Optimized time (ns): \$time2")
    println("Improvement: \${(time1 - time2) / 1000000.0} ms (\${(time1 - time2) * 100.0 / time1}%)")
}
