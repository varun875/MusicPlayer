package com.suspended.music.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suspended.music.data.model.Song
import com.suspended.music.ui.theme.HiResGold

/**
 * Displays a song's bitrate, sample rate, duration and file size.
 *
 * The formatted bitrate/sample-rate/duration strings are read straight off the
 * [Song] — they're cached on the model (computed once at construction), so there
 * is nothing to `remember` here. Because [Song] is `@Immutable`, Compose can
 * also skip this entire component when the song instance is unchanged.
 */
@Composable
fun AudioInfoDisplay(
    song: Song,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // The one value not cached on the model, so remember it by size.
    val formattedFileSize = remember(song.size) {
        Formatter.formatShortFileSize(context, song.size)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioInfoCard(
                label = "Bitrate",
                value = song.formattedBitrate,
                modifier = Modifier.weight(1f)
            )
            AudioInfoCard(
                label = "Sample Rate",
                value = song.formattedSampleRate,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioInfoCard(
                label = "Duration",
                value = song.formattedDuration,
                modifier = Modifier.weight(1f)
            )
            AudioInfoCard(
                label = "File Size",
                value = formattedFileSize,
                modifier = Modifier.weight(1f)
            )
        }

        if (song.isHiRes) {
            HiResBadge()
        }
    }
}

@Composable
private fun AudioInfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HiResBadge() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = HiResGold.copy(alpha = 0.15f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.HighQuality,
                // Decorative: the adjacent text already announces "Hi-Res Audio".
                contentDescription = null,
                tint = HiResGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hi-Res Audio",
                color = HiResGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
