package com.suspended.music.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
// import androidx.compose.foundation.draganddrop.dragAndDropSource
// import androidx.compose.foundation.draganddrop.dragAndDropTarget
// import androidx.compose.foundation.draganddrop.DragAndDropEvent
// import androidx.compose.foundation.draganddrop.DragAndDropTarget
// import androidx.compose.foundation.draganddrop.rememberDragAndDropState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import coil3.compose.AsyncImage
import com.suspended.music.data.model.Playlist
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.theme.CardShape
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MainViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    
    val playlist by viewModel.getPlaylist(playlistId).collectAsState(initial = null)
    val playlistSongs by viewModel.getPlaylistSongs(playlistId).collectAsState(initial = emptyList())
    
    // UI state
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf<List<Long>>(emptyList()) }
    var isSelecting by remember { mutableStateOf(false) }
    
    // Form state for edit dialog
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }
    
    LaunchedEffect(playlist) {
        playlist?.let {
            playlistName = it.name
            playlistDescription = it.description ?: ""
        }
    }
    
    val totalDuration = playlistSongs.sumOf { it.duration }
    val formattedDuration = formatDuration(totalDuration)
    
    Scaffold(
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
                    // Play/ Shuffle buttons
                    if (playlistSongs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                // Play entire playlist
                                if (playlistSongs.isNotEmpty()) {
                                    viewModel.playPlaylist(playlistSongs)
                                    onNavigateToNowPlaying()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        }
                        
                        IconButton(
                            onClick = {
                                // Shuffle playlist
                                if (playlistSongs.isNotEmpty()) {
                                    val shuffled = playlistSongs.shuffled()
                                    viewModel.playPlaylist(shuffled)
                                    onNavigateToNowPlaying()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                        }
                    }
                    
                    // More options
                    PlaylistOptionsMenu(
                        isSmartPlaylist = playlist?.isSmartPlaylist == true,
                        onEdit = { showEditDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onShare = {
                            if (playlistSongs.isNotEmpty()) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val uris = ArrayList<android.net.Uri>()
                                    for (song in playlistSongs) {
                                        val file = File(song.path)
                                        if (file.exists()) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            uris.add(uri)
                                        }
                                    }

                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        if (uris.isNotEmpty()) {
                                            val shareIntent = if (uris.size == 1) {
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "audio/*"
                                                    putExtra(Intent.EXTRA_STREAM, uris.first())
                                                }
                                            } else {
                                                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                                    type = "audio/*"
                                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                                }
                                            }
                                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            context.startActivity(Intent.createChooser(shareIntent, "Share playlist"))
                                        } else {
                                            snackbarHostState.showSnackbar("No valid songs found to share")
                                        }
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Playlist is empty")
                                }
                            }
                        }
                    )
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        floatingActionButton = {
            if (!isSelecting && playlist?.isSmartPlaylist != true) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSongsDialog = true },
                    text = { Text("Add Songs") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            // Add selected songs to another playlist
                            scope.launch {
                                snackbarHostState.showSnackbar("Add to playlist feature coming soon!")
                            }
                        }
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to...")
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            // Remove selected songs
                            scope.launch {
                                viewModel.removeSongsFromPlaylist(playlistId, selectedSongs)
                                selectedSongs = emptyList()
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Playlist Info Card
                PlaylistInfoCard(
                    playlist = playlist!!,
                    songCount = playlistSongs.size,
                    totalDuration = formattedDuration,
                    onEdit = { showEditDialog = true }
                )
                
                // Selection toggle
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
                        androidx.compose.material3.FilterChip(
                            selected = isSelecting,
                            onClick = { 
                                isSelecting = !isSelecting
                                if (!isSelecting) selectedSongs = emptyList()
                            },
                            label = { Text(if (isSelecting) "Cancel Selection" else "Select Songs") }
                        )
                        
                        if (isSelecting && selectedSongs.isNotEmpty()) {
                            Text(
                                text = "${selectedSongs.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Songs List
                if (playlistSongs.isEmpty()) {
                    EmptyState(
                        icon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(64.dp)) },
                        title = "Empty Playlist",
                        subtitle = "Add some songs to get started"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = playlistSongs,
                            key = { index, song -> song.id }
                        ) { index, song ->
                            val isSelected = selectedSongs.contains(song.id)
                            
                            PlaylistSongItem(
                                song = song,
                                index = index + 1,
                                isSelected = isSelected,
                                isSelecting = isSelecting,
                                onToggleSelection = { 
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                },
                                onPlay = {
                                    // Play from this song
                                    val remainingSongs = playlistSongs.subList(index, playlistSongs.size)
                                    viewModel.playPlaylist(remainingSongs)
                                    onNavigateToNowPlaying()
                                },
                                onMoreOptions = { /* TODO: Song options */ }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Edit Playlist Dialog
    if (showEditDialog) {
        EditPlaylistDialog(
            playlist = playlist!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                scope.launch {
                    viewModel.updatePlaylist(
                        playlist!!.copy(
                            name = name,
                            description = description ?: ""
                        )
                    )
                    snackbarHostState.showSnackbar("Playlist updated")
                }
                showEditDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeletePlaylistDialog(
            playlist = playlist!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    viewModel.deletePlaylist(playlistId)
                    snackbarHostState.showSnackbar("Playlist deleted")
                    onBackClick()
                }
                showDeleteDialog = false
            }
        )
    }
    
    // Add Songs Dialog
    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlistId = playlistId,
            viewModel = viewModel,
            onDismiss = { showAddSongsDialog = false },
            onSongsAdded = { count ->
                scope.launch {
                    snackbarHostState.showSnackbar("$count songs added to playlist")
                }
                showAddSongsDialog = false
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
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (!isSmartPlaylist) {
                DropdownMenuItem(
                    text = { Text("Edit Playlist") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
            }
            
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onShare()
                }
            )
            
            if (!isSmartPlaylist) {
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { 
                        Icon(
                            Icons.Filled.Delete, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        ) 
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistInfoCard(
    playlist: Playlist,
    songCount: Int,
    totalDuration: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!playlist.description.isNullOrBlank()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                if (!playlist.isSmartPlaylist) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit playlist",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoItem(
                    icon = Icons.Filled.MusicNote,
                    label = "Songs",
                    value = songCount.toString()
                )
                
                InfoItem(
                    icon = Icons.Filled.Schedule,
                    label = "Duration",
                    value = totalDuration
                )
                
                if (playlist.isSmartPlaylist) {
                    InfoItem(
                        icon = Icons.Filled.AutoAwesome,
                        label = "Type",
                        value = "Smart"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlaylistSongItem(
    song: Song,
    index: Int,
    isSelected: Boolean,
    isSelecting: Boolean,
    onToggleSelection: () -> Unit,
    onPlay: () -> Unit,
    onMoreOptions: () -> Unit
) {
    Surface(
        onClick = {
            if (isSelecting) {
                onToggleSelection()
            } else {
                onPlay()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isSelecting) it else it.pointerInput(Unit) {} },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelecting) {
                // Selection checkbox
                Surface(
                    onClick = onToggleSelection,
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = if (!isSelected) {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    } else null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                // Song number
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Album art
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CardShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Duration
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // More options
            IconButton(onClick = onMoreOptions) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EditPlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var description by remember { mutableStateOf(playlist.description ?: "") }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Playlist") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = {
            Text("Are you sure you want to delete '${playlist.name}'? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddSongsToPlaylistDialog(
    playlistId: Long,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSongsAdded: (Int) -> Unit
) {
    // TODO: Implement song selection dialog
    // For now, show a simple dialog
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs") },
        text = {
            Text("Song selection feature coming soon! This will allow you to browse your library and add songs to this playlist.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Helper functions
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    
    return when {
        hours > 0 -> String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

// Missing icons for reference
object Icons {
    val Schedule = Icons.Filled.Schedule
    val AutoAwesome = Icons.Filled.AutoAwesome
    val Check = Icons.Filled.Check
    val MusicNote = Icons.Filled.MusicNote
}