package com.suspended.music.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MimeTypes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * A song/track in the music library.
 *
 * Persisted by Room (table `songs`) and used directly as a Compose UI model,
 * hence [@Immutable][Immutable]: every field is a `val`, so Compose treats
 * instances as stable and skips recomposition when the reference is unchanged.
 *
 * Expensive derived values (URI parsing, number formatting, extension mapping)
 * are computed once at construction and cached — they're pure functions of
 * immutable state, and in a scrolling list they'd otherwise be recomputed many
 * times per frame. Trivial derivations ([isHiRes]) stay computed.
 */
@Immutable
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["albumId"]),    // album detail queries
        Index(value = ["artist"]),     // artist queries
        Index(value = ["album"]),      // album-name queries
        Index(value = ["genre"]),      // genre queries
        Index(value = ["isFavorite"]), // ⚠ see index audit below — likely drop
        Index(value = ["dateAdded"]),  // recently added
        Index(value = ["dateModified"]),// sort-by-modified queries
        Index(value = ["lastPlayed"]), // recently played
        Index(value = ["playCount"]),  // most played
        Index(value = ["title"]),      // ⚠ only helps prefix search — see below
        Index(value = ["duration"]),  // ⚠ see index audit below — likely drop
        Index(value = ["path"]),      // folder filtering / DISTINCT path queries
        Index(value = ["size"])       // sort-by-size paging
    ]
)
data class Song(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long, // milliseconds
    val path: String,
    val uri: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    @ColumnInfo(defaultValue = "''")
    val composer: String = "",
    val bitrate: Int = 0, // kbps
    val sampleRate: Int = 0, // Hz
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val size: Long = 0, // bytes
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val lyrics: String? = null
) {
    // ── Cached derived values ────────────────────────────────────────────
    // Body properties → excluded from the data class's equals/hashCode/copy,
    // and @Ignore keeps Room from persisting the new backing fields.
    @Ignore val albumArtUri: Uri = Uri.parse(ALBUM_ART_BASE + albumId)
    @Ignore val contentUri: Uri = Uri.parse(uri)
    @Ignore val formattedDuration: String = durationLabel(duration)
    @Ignore val formattedBitrate: String = bitrateLabel(bitrate)
    @Ignore val formattedSampleRate: String = sampleRateLabel(sampleRate)
    @Ignore val codec: String = codecOf(path)
    @Ignore val mimeType: String = mimeTypeOf(path)

    // Trivial — cheaper to compute than to fetch a cached field.
    val isHiRes: Boolean
        get() = sampleRate > 48000 || bitrate > 320

    private companion object {
        const val ALBUM_ART_BASE = "content://media/external/audio/albumart/"

        fun durationLabel(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

        fun bitrateLabel(kbps: Int): String =
            if (kbps > 0) "${kbps}kbps" else "Unknown"

        fun sampleRateLabel(hz: Int): String = when {
            hz >= 1000 -> {
                val khz = hz / 1000.0
                // Whole numbers render clean (48kHz); fractions keep one decimal (44.1kHz).
                if (khz == khz.toLong().toDouble()) "${khz.toLong()}kHz"
                else "${String.format(Locale.US, "%.1f", khz)}kHz"
            }
            hz > 0 -> "${hz}Hz"
            else -> "Unknown"
        }

        fun codecOf(path: String): String =
            when (val ext = path.substringAfterLast('.', "").lowercase()) {
                "flac" -> "FLAC"
                "mp3" -> "MP3"
                "aac", "m4a" -> "AAC"
                "ogg", "oga" -> "OGG Vorbis"
                "opus" -> "Opus"
                "wav" -> "WAV"
                "wma" -> "WMA"
                "aiff", "aif" -> "AIFF"
                "alac" -> "ALAC"
                "dsf", "dff" -> "DSD"
                else -> ext.uppercase().ifEmpty { "Unknown" }
            }

        /**
         * Maps the file extension to an audio MIME type so ExoPlayer can select
         * the right extractor + (FFmpeg) renderer without relying on content
         * sniffing, which often fails for AAC, M4A, Opus, OGG, etc.
         */
        fun mimeTypeOf(path: String): String =
            when (val ext = path.substringAfterLast('.', "").lowercase()) {
                "mp3" -> MimeTypes.AUDIO_MPEG
                "aac", "adts" -> MimeTypes.AUDIO_AAC
                "m4a", "m4b", "m4p", "mp4" -> MimeTypes.AUDIO_MP4
                "opus" -> MimeTypes.AUDIO_OPUS
                "ogg", "oga" -> MimeTypes.AUDIO_OGG
                "flac" -> MimeTypes.AUDIO_FLAC
                "wav" -> MimeTypes.AUDIO_WAV
                "wma" -> "audio/x-ms-wma"
                "aiff", "aif" -> "audio/x-aiff"
                "dsf", "dff" -> MimeTypes.AUDIO_DSD
                "amr" -> MimeTypes.AUDIO_AMR
                "mka" -> MimeTypes.AUDIO_MATROSKA
                "alac" -> MimeTypes.AUDIO_ALAC
                "ac3" -> MimeTypes.AUDIO_AC3
                "eac3" -> MimeTypes.AUDIO_E_AC3
                "ape" -> "audio/ape"
                "tta" -> "audio/tta"
                else -> MimeTypes.AUDIO_UNKNOWN
            }
    }
}
