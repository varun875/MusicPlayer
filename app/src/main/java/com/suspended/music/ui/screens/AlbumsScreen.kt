package com.suspended.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Album
import com.suspended.music.ui.components.AlphabetIndex
import com.suspended.music.ui.components.AlbumCard
import com.suspended.music.ui.components.AlbumItem
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.LibrarySectionHeader
import com.suspended.music.ui.utils.rememberAdaptiveLayoutInfo
import com.suspended.music.ui.utils.rememberReducedMotion
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private sealed interface AlbumRow {
    data class Header(val letter: Char) : AlbumRow
    data class Entry(val album: Album) : AlbumRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val libraryConfig = appSettings.library
    val isGrid = libraryConfig.layoutMode == "GRID"
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    val reducedMotion = rememberReducedMotion()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    val sections: List<Pair<Char, List<Album>>> = remember(albums) {
        albums
            .sortedBy { it.title.lowercase() }
            .groupBy { it.title.firstOrNull()?.uppercaseChar()?.takeIf { c -> c in 'A'..'Z' } ?: '#' }
            .entries
            .sortedWith(compareBy({ it.key == '#' }, { it.key }))
            .map { it.key to it.value }
    }
    val rows: List<AlbumRow> = remember(sections) {
        buildList {
            sections.forEach { (letter, list) ->
                add(AlbumRow.Header(letter))
                list.forEach { add(AlbumRow.Entry(it)) }
            }
        }
    }
    val letters = remember(sections) { sections.map { it.first } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search albums"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.setLibraryLayoutMode(if (isGrid) "LIST" else "GRID")
                        }
                    ) {
                        Icon(
                            imageVector = if (isGrid) Icons.Filled.List else Icons.Filled.GridView,
                            contentDescription = if (isGrid) "Switch to list view" else "Switch to grid view"
                        )
                    }
                    AnimatedVisibility(
                        visible = isGrid,
                        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.6f),
                        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.6f)
                    ) {
                        IconButton(
                            onClick = {
                                val next = if (libraryConfig.gridSize >= 4) 2 else libraryConfig.gridSize + 1
                                viewModel.setLibraryGridSize(next)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = libraryConfig.gridSize.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (albums.isEmpty()) {
            EmptyState(
                icon = {
                    val pulse = rememberInfiniteTransition(label = "emptyPulse").animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .scale(if (reducedMotion) 1f else pulse.value)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    CircleShape
                                )
                        )
                        Icon(
                            imageVector = Icons.Filled.Album,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = "Your library is quiet",
                subtitle = "Run a scan to pull every album on this device into Pulse.",
                modifier = Modifier.padding(paddingValues),
                action = {
                    Button(
                        onClick = { viewModel.scanMedia() },
                        enabled = !isScanning
                    ) {
                        Text(if (isScanning) "Scanning…" else "Scan library")
                    }
                }
            )
        } else if (isGrid) {
            LazyVerticalGrid(
                columns = if (adaptiveInfo.isTwoPane) {
                    GridCells.Adaptive(minSize = 200.dp)
                } else {
                    GridCells.Fixed(libraryConfig.gridSize)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = albums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onNavigateToAlbum(album.id) })
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AlbumSectionedList(
                    rows = rows,
                    listState = listState,
                    onAlbumClick = { onNavigateToAlbum(it.id) }
                )

                if (letters.size > 1) {
                    AlphabetIndex(
                        letters = letters,
                        onLetterSelected = { letter ->
                            val target = rows.indexOfFirst {
                                it is AlbumRow.Header && it.letter == letter
                            }
                            if (target >= 0) {
                                scope.launch { listState.animateScrollToItem(target) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 100.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSectionedList(
    rows: List<AlbumRow>,
    listState: LazyListState,
    onAlbumClick: (Album) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is AlbumRow.Header -> "h_${row.letter}"
                    is AlbumRow.Entry -> "al_${row.album.id}"
                }
            },
            contentType = { row ->
                if (row is AlbumRow.Header) "header" else "album"
            }
        ) { row ->
            when (row) {
                is AlbumRow.Header -> LibrarySectionHeader(letter = row.letter.toString())
                is AlbumRow.Entry -> AlbumItem(
                    album = row.album,
                    onClick = { onAlbumClick(row.album) }
                )
            }
        }
    }
}
