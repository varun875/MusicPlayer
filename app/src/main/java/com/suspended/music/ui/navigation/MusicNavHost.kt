package com.suspended.music.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.suspended.music.ui.screens.PlaylistDetailScreen
import com.suspended.music.ui.viewmodel.MainViewModel
import com.suspended.music.ui.theme.SharedTransitionSpecs

@Composable
fun MusicNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Songs
        composable(Screen.Songs.route) {
            SongsScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
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
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
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
                onCreatePlaylist = { /* Show create playlist dialog */ }
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
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
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
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onNavigateToTagEditor = { songId -> navController.navigate(Screen.TagEditor.createRoute(songId)) },
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
