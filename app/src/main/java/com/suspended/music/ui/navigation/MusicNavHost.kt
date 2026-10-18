package com.suspended.music.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.suspended.music.ui.screens.AlbumDetailScreen
import com.suspended.music.ui.screens.ArtistDetailScreen
import com.suspended.music.ui.screens.TagEditorScreen
import com.suspended.music.ui.screens.AlbumsScreen
import com.suspended.music.ui.screens.ArtistsScreen
import com.suspended.music.ui.screens.HomeScreen
import com.suspended.music.ui.screens.NowPlayingScreen
import com.suspended.music.ui.screens.PlaylistsScreen
import com.suspended.music.ui.screens.SongsScreen
import com.suspended.music.ui.screens.EqualizerScreen
import com.suspended.music.ui.screens.SettingsScreen
import com.suspended.music.ui.screens.PlaylistsManagementScreen
import com.suspended.music.ui.screens.QueueScreen
import com.suspended.music.ui.screens.SearchScreen
import com.suspended.music.ui.screens.PlaylistDetailScreen
import com.suspended.music.ui.viewmodel.MainViewModel

@Composable
fun MusicNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val bottomRoutes = Screen.bottomNavItems.map { it.route }.toSet()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (from in bottomRoutes && to in bottomRoutes) {
                fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing))
            } else {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 8 },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
            }
        },
        exitTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (from in bottomRoutes && to in bottomRoutes) {
                fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
            } else {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 10 },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    )
            }
        },
        popEnterTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (from in bottomRoutes && to in bottomRoutes) {
                fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing))
            } else {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 8 },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
            }
        },
        popExitTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (from in bottomRoutes && to in bottomRoutes) {
                fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
            } else {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 10 },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    )
            }
        }
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSongs = { navController.navigate(Screen.Songs.route) },
                onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                onNavigateToArtists = { navController.navigate(Screen.Artists.route) },
                onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTagEditor = { songId -> navController.navigate(Screen.TagEditor.createRoute(songId)) }
            )
        }
        
        // Songs
        composable(Screen.Songs.route) {
            SongsScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToTagEditor = { songId ->
                    navController.navigate(Screen.TagEditor.createRoute(songId))
                }
            )
        }
        
        // Albums
        composable(Screen.Albums.route) {
            AlbumsScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                }
            )
        }
        
        // Album Detail
        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToTagEditor = { songId -> navController.navigate(Screen.TagEditor.createRoute(songId)) }
            )
        }

        // Artists
        composable(Screen.Artists.route) {
            ArtistsScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                }
            )
        }
        
        // Playlists
        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onCreatePlaylist = {
                    navController.navigate(Screen.PlaylistsManagement.route)
                }
            )
        }
        
        // Artist Detail
        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = artistId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
            )
        }

        // Tag Editor
        composable(
            route = Screen.TagEditor.route,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId") ?: return@composable
            TagEditorScreen(
                songId = songId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Now Playing
        composable(
            route = Screen.NowPlaying.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                )
            }
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onNavigateToTagEditor = { songId -> navController.navigate(Screen.TagEditor.createRoute(songId)) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        // Equalizer
        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Queue
        composable(Screen.Queue.route) {
            QueueScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Search
        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToTagEditor = { songId ->
                    navController.navigate(Screen.TagEditor.createRoute(songId))
                }
            )
        }

        // Playlist Detail
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToTagEditor = { songId ->
                    navController.navigate(Screen.TagEditor.createRoute(songId))
                }
            )
        }

        // Playlists Management
        composable(Screen.PlaylistsManagement.route) {
            PlaylistsManagementScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                }
            )
        }
    }
}
