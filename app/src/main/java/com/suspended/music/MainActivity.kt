package com.suspended.music

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.MoreExecutors
import com.suspended.music.player.PlaybackService
import com.suspended.music.ui.components.MiniPlayer
import com.suspended.music.ui.navigation.MusicNavHost
import com.suspended.music.ui.navigation.Screen
import com.suspended.music.ui.theme.MusicPlayerTheme
import com.suspended.music.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var mediaController: MediaController? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permissions granted, scan media
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request permissions
        requestPermissions()
        
        // Start the PlaybackService to enable audio playback
        startPlaybackService()
        
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.appSettings.collectAsState()

            MusicPlayerTheme(
                themeConfig = settings.theme,
                dynamicColor = settings.theme.useDynamicColors
            ) {
                MusicPlayerScreen(viewModel = viewModel)
            }
        }
    }
    
    private fun startPlaybackService() {
        // Create session token to connect to PlaybackService
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        
        // Build MediaController - this automatically starts the service
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }
    
    override fun onDestroy() {
        mediaController?.release()
        mediaController = null
        super.onDestroy()
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // For Android 13+, request READ_MEDIA_AUDIO (covers audio files)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11-12, request both MANAGE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE
            // MANAGE_EXTERNAL_STORAGE allows "All files access" to scan music from any location
            // READ_EXTERNAL_STORAGE is a fallback for compatibility
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            // For Android 10 and below, request READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun MusicPlayerScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom nav should be visible
    val shouldShowBottomBar by remember(currentRoute) {
        derivedStateOf {
            currentRoute in Screen.bottomNavItems.map { it.route }
        }
    }
    
    val currentSong by viewModel.currentSong.collectAsState()
    
    val shouldShowMiniPlayer by remember(currentRoute, currentSong) {
        derivedStateOf {
            currentRoute != Screen.NowPlaying.route && currentSong != null
        }
    }
    
    // Auto-scan on first launch
    LaunchedEffect(Unit) {
        viewModel.scanMedia()
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MusicBottomBar(
                navController = navController,
                navBackStackEntry = navBackStackEntry,
                shouldShowBottomBar = shouldShowBottomBar
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MusicNavHost(
                navController = navController,
                viewModel = viewModel
            )
            
            // Mini Player with isolated state to prevent full screen recomposition
            AnimatedVisibility(
                visible = shouldShowMiniPlayer,
                enter = slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniPlayerContainer(
                    viewModel = viewModel,
                    onClick = { navController.navigate(Screen.NowPlaying.route) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MusicBottomBar(
    navController: NavController,
    navBackStackEntry: NavBackStackEntry?,
    shouldShowBottomBar: Boolean
) {
    AnimatedVisibility(
        visible = shouldShowBottomBar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            tonalElevation = 0.dp
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = navBackStackEntry?.destination?.hierarchy?.any {
                    it.route == screen.route
                } == true

                NavigationBarItem(
                    icon = {
                        val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                        Icon(
                            imageVector = icon ?: Icons.Filled.Home,
                            contentDescription = screen.title
                        )
                    },
                    label = { Text(screen.title) },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun MiniPlayerContainer(
    viewModel: MainViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    
    val progress by remember(currentPosition, duration) {
        derivedStateOf {
            if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        }
    }
    
    MiniPlayer(
        currentSong = currentSong,
        isPlaying = isPlaying,
        progress = progress,
        onPlayPause = { viewModel.togglePlayPause() },
        onSkipNext = { viewModel.skipToNext() },
        onClick = onClick,
        modifier = modifier
    )
}
