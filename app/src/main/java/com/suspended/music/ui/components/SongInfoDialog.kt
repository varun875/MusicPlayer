package com.suspended.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suspended.music.data.model.Song
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val file = File(song.path)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Song Information",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // File information
                InfoSection(title = "File Details") {
                    InfoRow(label = "Title", value = song.title)
                    InfoRow(label = "Artist", value = song.artist)
                    InfoRow(label = "Album", value = song.album ?: "Unknown Album")
                    InfoRow(label = "Genre", value = song.genre ?: "Unknown Genre")
                    InfoRow(label = "Duration", value = formatDuration(song.duration))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File system information
                InfoSection(title = "File System") {
                    InfoRow(label = "File Name", value = file.name)
                    InfoRow(label = "File Size", value = formatFileSize(file.length()))
                    InfoRow(label = "Location", value = song.path)
                    if (file.exists()) {
                        InfoRow(
                            label = "Modified",
                            value = dateFormat.format(Date(file.lastModified()))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback statistics
                InfoSection(title = "Statistics") {
                    InfoRow(label = "Play Count", value = song.playCount.toString())
                    InfoRow(
                        label = "Last Played",
                        value = if (song.lastPlayed > 0) {
                            dateFormat.format(Date(song.lastPlayed))
                        } else {
                            "Never"
                        }
                    )
                    InfoRow(
                        label = "Date Added",
                        value = dateFormat.format(Date(song.dateAdded))
                    )
                    InfoRow(
                        label = "Favorite",
                        value = if (song.isFavorite) "Yes" else "No"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Album art information
                InfoSection(title = "Media") {
                    InfoRow(
                        label = "Album Art",
                        value = if (song.albumArtUri != null) "Available" else "Not available"
                    )
                    if (song.albumId != null) {
                        InfoRow(label = "Album ID", value = song.albumId.toString())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = milliseconds / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        else -> String.format("%.2f KB", kb)
    }
}
