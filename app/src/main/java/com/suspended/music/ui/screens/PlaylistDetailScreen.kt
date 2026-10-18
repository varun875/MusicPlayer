package com.suspended.music.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Playlist
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MainViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()

    val playlist by remember(playlistId) { viewModel.getPlaylist(playlistId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val playlistSongs by remember(playlistId) { viewModel.getPlaylistSongs(playlistId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.playlists.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showSongAddToPlaylistDialog by remember { mutableStateOf(false) }

    val existingIds = remember(playlistSongs) { playlistSongs.map { it.id }.toHashSet() }
    val songsToAdd = remember(allSongs, existingIds) { allSongs.filterNot { it.id in existingIds } }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (playlistSongs.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.playPlaylist(playlistSongs)
                            onNavigateToNowPlaying()
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(onClick = {
                            viewModel.playPlaylist(playlistSongs.shuffled())
                            onNavigateToNowPlaying()
                        }) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                        }
                    }
                    PlaylistOptionsMenu(
                        isSmartPlaylist = playlist?.isSmartPlaylist == true,
                        onEdit = { showEditDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onShare = {
                            playlist?.let { sharePlaylist(context, it, playlistSongs) }
                        }
                    )
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            )
        },
        floatingActionButton = {
            if (!isSelecting && playlist?.isSmartPlaylist != true) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSongsDialog = true },
                    text = { Text("Add Songs") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            } else if (isSelecting) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = selectedSongIds.isNotEmpty(),
                        onClick = { showCopyDialog = true }
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to...")
                    }
                    FilledTonalButton(
                        enabled = selectedSongIds.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                viewModel.removeSongsFromPlaylist(playlistId, selectedSongIds.toList())
                                selectedSongIds = emptySet()
                                isSelecting = false
                                snackbarHostState.showSnackbar("Songs removed from playlist")
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            }
        }
    ) { padding ->
        if (playlist == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PlaylistInfoCard(
                playlist = playlist!!,
                songCount = playlistSongs.size,
                duration = formatDuration(playlistSongs.sumOf { it.duration }),
                onEdit = { showEditDialog = true }
            )

            AnimatedVisibility(
                visible = playlistSongs.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isSelecting,
                        onClick = {
                            isSelecting = !isSelecting
                            if (!isSelecting) selectedSongIds = emptySet()
                        },
                        label = { Text(if (isSelecting) "Cancel Selection" else "Select Songs") }
                    )
                    if (isSelecting && selectedSongIds.isNotEmpty()) {
                        Text(
                            text = "${selectedSongIds.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (playlistSongs.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(64.dp)) },
                    title = "Empty Playlist",
                    subtitle = "Add some songs to get started"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    itemsIndexed(playlistSongs, key = { index, song -> "${index}_${song.id}" }) { index, song ->
                        if (isSelecting) {
                            SelectableSongRow(
                                song = song,
                                selected = song.id in selectedSongIds,
                                onToggle = {
                                    selectedSongIds = if (song.id in selectedSongIds) {
                                        selectedSongIds - song.id
                                    } else {
                                        selectedSongIds + song.id
                                    }
                                }
                            )
                        } else {
                            SongItem(
                                song = song,
                                index = index,
                                isPlaying = currentSong?.id == song.id,
                                onClick = {
                                    viewModel.playPlaylist(playlistSongs, index)
                                    onNavigateToNowPlaying()
                                },
                                onMoreClick = {
                                    selectedSong = it
                                    scope.launch { sheetState.show() }
                                },
                                onFavoriteClick = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && playlist != null) {
        EditPlaylistDialog(
            playlist = playlist!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                viewModel.updatePlaylist(playlist!!.copy(name = name, description = description))
                showEditDialog = false
                scope.launch { snackbarHostState.showSnackbar("Playlist updated") }
            }
        )
    }

    if (showDeleteDialog && playlist != null) {
        DeletePlaylistDialog(
            playlist = playlist!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deletePlaylist(playlistId)
                showDeleteDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Playlist deleted")
                    onBackClick()
                }
            }
        )
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            songs = songsToAdd,
            onDismiss = { showAddSongsDialog = false },
            onAdd = { songIds ->
                viewModel.addSongsToPlaylist(playlistId, songIds)
                showAddSongsDialog = false
                scope.launch { snackbarHostState.showSnackbar("${songIds.size} songs added") }
            }
        )
    }

    if (showCopyDialog) {
        PlaylistPickerDialog(
            playlists = allPlaylists.filterNot { it.isSmartPlaylist || it.id == playlistId },
            title = "Add selected songs to playlist",
            emptyMessage = "No other editable playlists available.",
            onDismiss = { showCopyDialog = false },
            onPlaylistSelected = { target ->
                viewModel.addSongsToPlaylist(target.id, selectedSongIds.toList())
                selectedSongIds = emptySet()
                isSelecting = false
                showCopyDialog = false
                scope.launch { snackbarHostState.showSnackbar("Added to ${target.name}") }
            }
        )
    }

    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }
                if (!showSongInfoDialog && !showSongAddToPlaylistDialog) {
                    selectedSong = null
                }
            },
            onDelete = { viewModel.deleteSong(song) },
            onAddToPlaylist = { showSongAddToPlaylistDialog = true },
            onEdit = { onNavigateToTagEditor(song.id) },
            onShowInfo = { showSongInfoDialog = true }
        )
    }

    if (showSongInfoDialog && selectedSong != null) {
        SongInfoDialog(
            song = selectedSong!!,
            onDismiss = {
                showSongInfoDialog = false
                if (!showSongAddToPlaylistDialog) {
                    selectedSong = null
                }
            }
        )
    }

    if (showSongAddToPlaylistDialog && selectedSong != null) {
        PlaylistPickerDialog(
            playlists = allPlaylists.filterNot { it.isSmartPlaylist },
            title = "Add '${selectedSong!!.title}' to playlist",
            emptyMessage = "Create a playlist first to add songs.",
            onDismiss = {
                showSongAddToPlaylistDialog = false
                selectedSong = null
            },
            onPlaylistSelected = { target ->
                viewModel.addSongsToPlaylist(target.id, listOf(selectedSong!!.id))
                showSongAddToPlaylistDialog = false
                selectedSong = null
                scope.launch { snackbarHostState.showSnackbar("Added to ${target.name}") }
            }
        )
    }
}

@Composable
private fun PlaylistOptionsMenu(
    isSmartPlaylist: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (!isSmartPlaylist) {
                DropdownMenuItem(
                    text = { Text("Edit Playlist") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { expanded = false; onEdit() }
                )
            }
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = { expanded = false; onShare() }
            )
            if (!isSmartPlaylist) {
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { expanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun PlaylistInfoCard(playlist: Playlist, songCount: Int, duration: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (playlist.description.isNotBlank()) {
                        Text(playlist.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!playlist.isSmartPlaylist) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit playlist")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Songs: $songCount")
                Text("Duration: $duration")
                if (playlist.isSmartPlaylist) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Smart")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableSongRow(song: Song, selected: Boolean, onToggle: () -> Unit) {
    Surface(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${song.artist} • ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditPlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var description by remember { mutableStateOf(playlist.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, description) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeletePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Delete '${playlist.name}'? This cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddSongsToPlaylistDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val filtered = remember(query, songs) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs") },
        text = {
            if (songs.isEmpty()) {
                Text("All songs are already in this playlist.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search songs") },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(items = filtered, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (song.id in selectedIds) {
                                            selectedIds - song.id
                                        } else {
                                            selectedIds + song.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = song.id in selectedIds,
                                    onCheckedChange = {
                                        selectedIds = if (song.id in selectedIds) {
                                            selectedIds - song.id
                                        } else {
                                            selectedIds + song.id
                                        }
                                    }
                                )
                                Column {
                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${song.artist} • ${song.album}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(selectedIds.toList()) }, enabled = selectedIds.isNotEmpty()) {
                Text("Add (${selectedIds.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

private fun sharePlaylist(context: Context, playlist: Playlist, songs: List<Song>) {
    val body = buildString {
        appendLine("Playlist: ${playlist.name}")
        if (playlist.description.isNotBlank()) appendLine(playlist.description)
        appendLine("Songs: ${songs.size}")
        appendLine()
        songs.take(100).forEachIndexed { index, song ->
            appendLine("${index + 1}. ${song.title} - ${song.artist}")
        }
        if (songs.size > 100) appendLine("...and ${songs.size - 100} more songs")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Playlist: ${playlist.name}")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share playlist")) }
}
