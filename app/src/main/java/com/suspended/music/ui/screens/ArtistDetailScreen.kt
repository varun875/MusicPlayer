package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    viewModel: MainViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allArtists by viewModel.artists.collectAsState()
    val artist = allArtists.find { it.id == artistId }

    val artistSongs by viewModel.getSongsByArtist(artist?.name ?: "").collectAsState(initial = emptyList())

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(artist?.name ?: "Artist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(
                items = artistSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    song = song,
                    index = index,
                    onClick = {
                        viewModel.playSongs(artistSongs, index)
                        onNavigateToNowPlaying()
                    },
                    isPlaying = false,
                    onMoreClick = { /* Show options */ },
                    onFavoriteClick = { viewModel.toggleFavorite(song) }
                )
            }
        }
    }
}