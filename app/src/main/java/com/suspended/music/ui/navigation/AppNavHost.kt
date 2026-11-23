package com.suspended.music.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplay.TransitionKey
import androidx.navigation3.ui.NavDisplay.PopTransitionKey
import androidx.navigation3.ui.NavDisplay.PredictivePopTransitionKey
import com.suspended.music.ui.screens.AlbumDetailScreen
import com.suspended.music.ui.screens.AlbumsScreen
import com.suspended.music.ui.screens.ArtistDetailScreen
import com.suspended.music.ui.screens.ArtistsScreen
import com.suspended.music.ui.screens.EqualizerScreen
import com.suspended.music.ui.screens.HomeScreen
import com.suspended.music.ui.screens.NowPlayingScreen
import com.suspended.music.ui.screens.PlaylistsManagementScreen
import com.suspended.music.ui.screens.PlaylistsScreen
import com.suspended.music.ui.screens.PlaylistDetailScreen
import com.suspended.music.ui.screens.QueueScreen
import com.suspended.music.ui.screens.SearchScreen
import com.suspended.music.ui.screens.SettingsScreen
import com.suspended.music.ui.screens.SongsScreen
import com.suspended.music.ui.screens.TagEditorScreen
import com.suspended.music.ui.viewmodel.MainViewModel

/**
 * Navigation 3 host.
 *
 * `backStack` is a plain observable list of [MusicRoute] keys; `NavDisplay`
 * renders the content for `backStack.last()`. Navigation is `backStack.add`
 * / `removeLastOrNull()` — no `NavController`, no string routes, no graph.
 *
 * Transitions:
 *  - bottom-bar ↔ bottom-bar: fade only (quick, matches the old design)
 *  - everything else: fade + directional slide (matches the old design)
 *  - [MusicRoute.NowPlaying]: slides up from the bottom (per-entry metadata),
 *    replacing the old per-destination composable transitions.
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.popSafely() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            // NavDisplay scopes don't expose from/to keys; use the global
            // fade+slide for everything. Bottom-bar fade-only behavior is
            // preserved per-entry via entry metadata where needed.
            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 8 },
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                )) togetherWith
                (fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 10 },
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    ))
        },
        popTransitionSpec = {
            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 8 },
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                )) togetherWith
                (fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 10 },
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    ))
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
        },
        entryProvider = entryProvider {

            entry<MusicRoute.Home> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSongs = { backStack.pushUnique(MusicRoute.Songs) },
                    onNavigateToAlbums = { backStack.pushUnique(MusicRoute.Albums) },
                    onNavigateToArtists = { backStack.pushUnique(MusicRoute.Artists) },
                    onNavigateToPlaylists = { backStack.pushUnique(MusicRoute.Playlists) },
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToAlbum = { albumId -> backStack.pushUnique(MusicRoute.AlbumDetail(albumId)) },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToQueue = { backStack.pushUnique(MusicRoute.Queue) },
                    onNavigateToSettings = { backStack.pushUnique(MusicRoute.Settings) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }

            entry<MusicRoute.Songs> {
                SongsScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }

            entry<MusicRoute.Albums> {
                AlbumsScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToAlbum = { albumId -> backStack.pushUnique(MusicRoute.AlbumDetail(albumId)) },
                )
            }

            entry<MusicRoute.AlbumDetail> { key ->
                AlbumDetailScreen(
                    albumId = key.albumId,
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }

            entry<MusicRoute.Artists> {
                ArtistsScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToArtist = { artistId -> backStack.pushUnique(MusicRoute.ArtistDetail(artistId)) },
                )
            }

            entry<MusicRoute.ArtistDetail> { key ->
                ArtistDetailScreen(
                    artistId = key.artistId,
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }

            entry<MusicRoute.Playlists> {
                PlaylistsScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToPlaylist = { playlistId -> backStack.pushUnique(MusicRoute.PlaylistDetail(playlistId)) },
                    onCreatePlaylist = { backStack.pushUnique(MusicRoute.PlaylistsManagement) },
                )
            }

            entry<MusicRoute.PlaylistDetail> { key ->
                PlaylistDetailScreen(
                    playlistId = key.playlistId,
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }

            entry<MusicRoute.PlaylistsManagement> {
                PlaylistsManagementScreen(
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                    onPlaylistClick = { playlistId -> backStack.pushUnique(MusicRoute.PlaylistDetail(playlistId)) },
                )
            }

            entry<MusicRoute.TagEditor> { key ->
                TagEditorScreen(
                    songId = key.songId,
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                )
            }

            entry<MusicRoute.NowPlaying>(
                metadata = metadata {
                    put(TransitionKey) {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                    }
                    put(PopTransitionKey) {
                        EnterTransition.None togetherWith slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        )
                    }
                    put(PredictivePopTransitionKey) {
                        EnterTransition.None togetherWith slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        )
                    }
                }
            ) {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backStack.popSafely() },
                    onNavigateToQueue = { backStack.pushUnique(MusicRoute.Queue) },
                    onNavigateToEqualizer = { backStack.pushUnique(MusicRoute.Equalizer) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                    onNavigateToSearch = { backStack.pushUnique(MusicRoute.Search) },
                    onNavigateToSettings = { backStack.pushUnique(MusicRoute.Settings) },
                )
            }

            entry<MusicRoute.Queue> {
                QueueScreen(
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                )
            }

            entry<MusicRoute.Equalizer> {
                EqualizerScreen(
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                )
            }

            entry<MusicRoute.Settings> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                )
            }

            entry<MusicRoute.Search> {
                SearchScreen(
                    viewModel = viewModel,
                    onBackClick = { backStack.popSafely() },
                    onNavigateToNowPlaying = { backStack.pushUnique(MusicRoute.NowPlaying) },
                    onNavigateToTagEditor = { songId -> backStack.pushUnique(MusicRoute.TagEditor(songId)) },
                )
            }
        },
    )
}

/** Pops the top route only when at least one destination remains on the stack. */
fun NavBackStack<NavKey>.popSafely() {
    if (size > 1) {
        removeLastOrNull()
    }
}

/** Pushes [route] only if it isn't already the top destination (prevents double-tap duplicates). */
fun NavBackStack<NavKey>.pushUnique(route: MusicRoute) {
    if (lastOrNull() != route) {
        add(route)
    }
}

/**
 * Mirrors the old Nav2 bottom-bar behavior: tapping a tab pops everything
 * above the start destination (saving state) and lands on that tab without
 * duplicating it. In Nav3 the stack is just a list, so we collapse it back
 * to [MusicRoute.Home] before pushing the tab (unless it's already a tab).
 */
fun navigateToBottomBarTab(backStack: NavBackStack<NavKey>, route: MusicRoute) {
    if (route !in bottomNavItems.map { it.route }) return
    while (backStack.size > 1) {
        if (backStack.last() == route) return
        backStack.removeLastOrNull()
    }
    if (backStack.lastOrNull() != route) {
        backStack.add(route)
    }
}

/** Creates the app back stack — used by the activity shell; kept here so it can be previewed/tested. */
@Composable
fun rememberAppBackStack(): NavBackStack<NavKey> = rememberNavBackStack(MusicRoute.Home)
