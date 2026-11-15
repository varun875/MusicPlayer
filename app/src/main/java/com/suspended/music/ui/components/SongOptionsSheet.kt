package com.suspended.music.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.suspended.music.data.model.Song
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEdit: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with song info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Options
            SheetOption(
                icon = Icons.Filled.PlaylistAdd,
                title = "Add to playlist",
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )

            SheetOption(
                icon = Icons.Filled.Edit,
                title = "Edit tags",
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )

            SheetOption(
                icon = Icons.Filled.Share,
                title = "Share",
                onClick = {
                    // Staging copies the whole file — do it off the main
                    // thread so a large FLAC can't jank/ANR the UI.
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            try {
                                val src = File(song.path)
                                if (!src.exists() || !src.isFile || !src.canRead()) return@withContext null
                                val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
                                val ext = src.extension.takeIf { it.isNotBlank() } ?: "mp3"
                                val staged = File(shareDir, "share_${System.currentTimeMillis()}.$ext")
                                src.inputStream().use { input ->
                                    staged.outputStream().use { output -> input.copyTo(output) }
                                }
                                FileProvider.getUriForFile(
                                    context, "${context.packageName}.provider", staged
                                )
                            } catch (_: Exception) {
                                null
                            }
                        } ?: return@launch
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = android.content.ClipData.newUri(context.contentResolver, "audio", uri)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share song"))
                    }
                    onDismiss()
                }
            )

            SheetOption(
                icon = Icons.Filled.Info,
                title = "Song info",
                onClick = {
                    onShowInfo()
                    onDismiss()
                }
            )

            SheetOption(
                icon = Icons.Filled.Delete,
                title = "Delete",
                destructive = true,
                onClick = {
                    showDeleteDialog = true
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete song?") },
            text = {
                Text("\"${song.title}\" will be permanently deleted from your device. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
