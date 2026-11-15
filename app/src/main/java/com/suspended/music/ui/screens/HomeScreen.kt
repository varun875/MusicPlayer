package com.suspended.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.theme.CardShape
import com.suspended.music.ui.utils.rememberHapticFeedback
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTagEditor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()

    val hapticFeedback = rememberHapticFeedback()

    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var showGenresDialog by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Active queue or fallback preview list for the hero carousel
    val queueDisplayList = remember(queue, allSongs) {
        if (queue.isNotEmpty()) queue else allSongs.take(15)
    }

    // Recently played items or fallback
    val recentDisplayList = remember(recentlyPlayed, allSongs) {
        if (recentlyPlayed.isNotEmpty()) recentlyPlayed else allSongs.take(15)
    }

    val isDark = isSystemInDarkTheme()
    val contentBlur by animateDpAsState(
        targetValue = if (drawerState.isOpen || drawerState.isAnimationRunning) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "drawer_content_blur"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(285.dp)
                    .fillMaxHeight(),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xF0181A22),
                                        Color(0xEA14161E),
                                        Color(0xF5101117)
                                    )
                                } else {
                                    listOf(
                                        Color(0xF5FFFFFF),
                                        Color(0xF0F6F7FB),
                                        Color(0xF8ECEFF6)
                                    )
                                }
                            )
                        )
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                ) {
                    SideNavigationDrawerContent(
                        onItemClick = { item ->
                            scope.launch { drawerState.close() }
                            hapticFeedback.lightTap()
                            when (item) {
                                DrawerItemType.PLAY_QUEUE -> onNavigateToQueue()
                                DrawerItemType.PLAYLISTS -> onNavigateToPlaylists()
                                DrawerItemType.SETTINGS -> onNavigateToSettings()
                            }
                        }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .blur(contentBlur)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ══════════════════════════════════════════════
                // 1. Listen Now header (Apple style)
                // ══════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                hapticFeedback.lightTap()
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { hapticFeedback.lightTap(); onNavigateToSearch() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Box {
                                var showMoreMenu by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { hapticFeedback.lightTap(); showMoreMenu = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Genres") },
                                        onClick = { showMoreMenu = false; showGenresDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Google Drive") },
                                        onClick = { showMoreMenu = false; showDriveDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Help") },
                                        onClick = { showMoreMenu = false; showHelpDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Listen Now",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Featured card — first item of queue/recents, full-width art
                val featuredSong = queueDisplayList.firstOrNull()
                if (featuredSong != null) {
                    Card(
                        onClick = {
                            hapticFeedback.lightTap()
                            viewModel.playSong(featuredSong)
                            onNavigateToNowPlaying()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(featuredSong.albumArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = featuredSong.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(CardShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = featuredSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = featuredSong.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

            // ══════════════════════════════════════════════
            // 2. "Play queue" Horizontal Carousel
            // ══════════════════════════════════════════════
            if (queueDisplayList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queueDisplayList, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isCurrentPlaying = currentSong?.id == song.id
                        QueueTrackCard(
                            song = song,
                            isCurrentPlaying = isCurrentPlaying,
                            isPlaying = isPlaying,
                            onClick = {
                                hapticFeedback.lightTap()
                                viewModel.playSong(song)
                                onNavigateToNowPlaying()
                            },
                            onOptionsClick = {
                                hapticFeedback.lightTap()
                                songForOptions = song
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ══════════════════════════════════════════════
            // 3. "Recently played" Section with FAB
            // ══════════════════════════════════════════════
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (recentDisplayList.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 12.dp, end = 80.dp, top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentDisplayList, key = { "recent_${it.id}" }) { song ->
                                RecentlyPlayedCard(
                                    song = song,
                                    onClick = {
                                        hapticFeedback.lightTap()
                                        viewModel.playSong(song)
                                        onNavigateToNowPlaying()
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Purple Shuffle / Smart Play Action Button
                FloatingActionButton(
                    onClick = {
                        hapticFeedback.mediumTap()
                        viewModel.shuffleAndPlay()
                        onNavigateToNowPlaying()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(56.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle playback",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Space at bottom for the docked MiniPlayer and NavigationBar
            Spacer(modifier = Modifier.height(110.dp))
            }
        }

        // ── Options Sheet ──
        songForOptions?.let { song ->
            SongOptionsSheet(
                song = song,
                sheetState = sheetState,
                onDismiss = { songForOptions = null },
                onDelete = {
                    viewModel.deleteSong(song)
                    songForOptions = null
                },
                onAddToPlaylist = {
                    songForPlaylist = song
                    songForOptions = null
                },
                onEdit = {
                    songForOptions = null
                    onNavigateToTagEditor(song.id)
                },
                onShowInfo = { songForOptions = null }
            )
        }

        // ── Add to Playlist Dialog ──
        songForPlaylist?.let { song ->
            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
            val editablePlaylists = remember(playlists) { playlists.filterNot { it.isSmartPlaylist } }
            PlaylistPickerDialog(
                playlists = editablePlaylists,
                title = "Add '${song.title}' to playlist",
                emptyMessage = "Create a playlist first.",
                onDismiss = { songForPlaylist = null },
                onPlaylistSelected = { playlist ->
                    viewModel.addSongsToPlaylist(playlist.id, listOf(song.id))
                    songForPlaylist = null
                }
            )
        }

        // ── Genres Dialog ──
        if (showGenresDialog) {
            // Single group-by pass — counting per row inside item content is
            // O(genres × songs) string work on every recomposition.
            val genreCounts = remember(allSongs) {
                allSongs.groupBy { it.genre.ifBlank { "Unknown Genre" } }
                    .mapValuesTo(LinkedHashMap()) { it.value.size }
                    .entries.sortedBy { it.key }
            }
            AlertDialog(
                onDismissRequest = { showGenresDialog = false },
                title = { Text("Genres", style = MaterialTheme.typography.titleLarge) },
                text = {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(genreCounts) { (genre, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showGenresDialog = false
                                        val songInGenre = allSongs.firstOrNull { (it.genre.ifBlank { "Unknown Genre" }) == genre }
                                        songInGenre?.let {
                                            viewModel.playSong(it)
                                            onNavigateToNowPlaying()
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(genre, style = MaterialTheme.typography.bodyLarge)
                                Text("$count tracks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGenresDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // ── Google Drive Dialog ──
        if (showDriveDialog) {
            AlertDialog(
                onDismissRequest = { showDriveDialog = false },
                title = { Text("Cloud / Google Drive") },
                text = {
                    Text("Cloud sync & Google Drive backup integration is coming in an upcoming update.")
                },
                confirmButton = {
                    TextButton(onClick = { showDriveDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // ── Help Dialog ──
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Pulse Music") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Version 1.7.0", fontWeight = FontWeight.SemiBold)
                        Text("High fidelity, fluid offline music player for Android.")
                        Text("Swipe horizontal on album art to change songs; drag seekbar to scrub live.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

// ══════════════════════════════════════════════
// Side Navigation Drawer Content
// ══════════════════════════════════════════════

private enum class DrawerItemType { PLAY_QUEUE, PLAYLISTS, SETTINGS }

@Composable
private fun SideNavigationDrawerContent(
    onItemClick: (DrawerItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Drawer holds only destinations that are NOT already in the bottom bar.
        DrawerRow(
            label = "Play queue",
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            iconColor = MaterialTheme.colorScheme.secondary,
            onClick = { onItemClick(DrawerItemType.PLAY_QUEUE) }
        )

        DrawerRow(
            label = "Playlists",
            icon = Icons.AutoMirrored.Filled.List,
            iconColor = MaterialTheme.colorScheme.primary,
            onClick = { onItemClick(DrawerItemType.PLAYLISTS) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        DrawerRow(
            label = "Settings",
            icon = Icons.Filled.Settings,
            iconColor = MaterialTheme.colorScheme.tertiary,
            onClick = { onItemClick(DrawerItemType.SETTINGS) }
        )
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ══════════════════════════════════════════════
// Queue Card (Matching Light/Dark Reference)
// ══════════════════════════════════════════════

@Composable
private fun QueueTrackCard(
    song: Song,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(true)
            .size(300, 300)
            .memoryCacheKey("queue_card_${song.albumId}")
            .diskCacheKey("queue_card_${song.albumId}")
            .build()
    }

    Card(
        onClick = onClick,
        modifier = modifier.width(132.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Square Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Playing indicator on bottom-left of artwork
                if (isCurrentPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Playing indicator",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Lower Metadata Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3-dots Menu Button
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// Recently Played Square Card
// ══════════════════════════════════════════════

@Composable
private fun RecentlyPlayedCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(true)
            .size(260, 260)
            .memoryCacheKey("recent_card_${song.albumId}")
            .diskCacheKey("recent_card_${song.albumId}")
            .build()
    }

    Card(
        onClick = onClick,
        modifier = modifier.size(118.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
