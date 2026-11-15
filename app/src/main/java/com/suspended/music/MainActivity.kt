package com.suspended.music

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.google.common.util.concurrent.MoreExecutors
import com.suspended.music.player.PlaybackService
import com.suspended.music.ui.components.MiniPlayer
import com.suspended.music.ui.components.NowPlayingPane
import com.suspended.music.ui.navigation.AppNavHost
import com.suspended.music.ui.navigation.MusicRoute
import com.suspended.music.ui.navigation.bottomNavItems
import com.suspended.music.ui.navigation.isBottomBarRoute
import com.suspended.music.ui.navigation.navigateToBottomBarTab
import com.suspended.music.ui.theme.MusicPlayerTheme
import com.suspended.music.ui.utils.MaxContentWidth
import com.suspended.music.ui.utils.WindowWidthSizeClass
import com.suspended.music.ui.utils.rememberAdaptiveLayoutInfo
import com.suspended.music.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null
    @Volatile private var destroyed = false
    private val viewModel: MainViewModel by viewModels()
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var resumeScanTriggered = false
    private var pendingAudioUri: Uri? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Tag writes via java.io need all-files access on Android 11+,
            // including on 13+ where READ_MEDIA_AUDIO alone is insufficient.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestManageStorage()
            }
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User returned from the All Files Access settings screen.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingAudioUri = extractAudioUri(intent)

        requestPermissions()
        startPlaybackService()

        setContent {
            val vm = viewModel
            val settings by vm.appSettings.collectAsStateWithLifecycle()

            MusicPlayerTheme(
                themeConfig = settings.theme,
                dynamicColor = settings.theme.useDynamicColors
            ) {
                MusicPlayerScreen(viewModel = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // One-shot: if the library is still empty after the first permission grant,
        // trigger a scan. Don't repeat on every resume — that interferes with manual
        // rescans and causes busy-looping when the device has no music files.
        if (!resumeScanTriggered) {
            val vm = viewModel
            if (vm.songCount.value == 0 && !vm.isScanning.value && hasStoragePermission()) {
                resumeScanTriggered = true
                vm.scanMedia()
            }
        }
        tryPlayPendingAudio()
    }
    
    private fun startPlaybackService() {
        // Create session token to connect to PlaybackService
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        
        // Build MediaController - this automatically starts the service
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future
        // Direct executor: the listener runs on the completing binder thread.
        // Guard against the activity being destroyed (e.g. rotation) before
        // the future resolves — otherwise the controller leaks its binding.
        future.addListener({
            try {
                val controller = future.get()
                if (destroyed) {
                    controller.release()
                } else {
                    mediaController = controller
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "MediaController build failed", e)
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        destroyed = true
        controllerFuture?.cancel(true)
        mediaController?.release()
        mediaController = null
        super.onDestroy()
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: scoped READ_MEDIA_AUDIO covers audio reads, but
            // TagRepository writes files directly via java.io, which needs
            // MANAGE_EXTERNAL_STORAGE — that can't be granted via runtime
            // permissions, so send the user to the Settings screen.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12: MANAGE_EXTERNAL_STORAGE is required for broad file access.
            // It CANNOT be granted via RequestMultiplePermissions — it requires the
            // Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION intent.
            if (!Environment.isExternalStorageManager()) {
                requestManageStorage()
            }
        } else {
            // Android 10 and below: direct tag writes need WRITE_EXTERNAL_STORAGE,
            // and pre-scoped-storage scanning needs legacy storage opt-in.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestManageStorage() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            manageStorageLauncher.launch(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAudioUri = extractAudioUri(intent)
        tryPlayPendingAudio()
    }

    /** Validates an ACTION_VIEW audio intent and returns its URI, or null if unusable. */
    private fun extractAudioUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        val mime = intent.type ?: contentResolver.getType(data) ?: ""
        if (!mime.startsWith("audio/") && !mime.startsWith("application/octet-stream")) return null
        if (data.scheme != "content" && data.scheme != "file") return null
        return data
    }

    /** Plays a pending "open with" URI once the ViewModel is available. */
    private fun tryPlayPendingAudio() {
        val uri = pendingAudioUri ?: return
        pendingAudioUri = null
        viewModel.playAudioUri(uri)
    }

    private fun hasStoragePermission(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Environment.isExternalStorageManager()
        else ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun MusicPlayerScreen(viewModel: MainViewModel) {
    val backStack = rememberNavBackStack(MusicRoute.Home)
    val currentKey = backStack.lastOrNull() as? MusicRoute

    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    // Surfaces scan/tag/playback errors with a recovery prompt.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Scoped-storage delete: the ViewModel emits the system consent intent, we
    // launch it, and the row is only dropped once the user approves.
    var pendingDeleteSongId by remember { mutableStateOf<Long?>(null) }
    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingDeleteSongId?.let { viewModel.confirmSongDeletion(it) }
        }
        pendingDeleteSongId = null
    }
    LaunchedEffect(Unit) {
        viewModel.deleteConsentRequest.collect { request ->
            pendingDeleteSongId = request.songId
            deleteConsentLauncher.launch(
                androidx.activity.result.IntentSenderRequest
                    .Builder(request.intent.intentSender)
                    .build()
            )
        }
    }

    val shouldShowBottomBar by remember(currentKey) {
        derivedStateOf { isBottomBarRoute(currentKey) }
    }

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val shouldShowMiniPlayer by remember(currentKey, currentSong, adaptiveInfo.isTwoPane) {
        derivedStateOf {
            // The right-hand pane covers the mini player on two-pane layouts.
            currentKey != MusicRoute.NowPlaying && currentSong != null && !adaptiveInfo.isTwoPane
        }
    }

    LaunchedEffect(currentKey, currentSong) {
        if (currentKey is MusicRoute.NowPlaying && currentSong == null) {
            // If Now Playing is restored without an active song, route back to Home.
            delay(350)
            val stillOnNowPlaying = backStack.lastOrNull() is MusicRoute.NowPlaying
            if (stillOnNowPlaying && currentSong == null) {
                backStack.removeLastOrNull()
                if (backStack.isEmpty()) {
                    backStack.add(MusicRoute.Home)
                }
            }
        }
    }

    LibraryScanBootstrap(viewModel = viewModel)

    when {
        // Expanded/Wide: rail + list column + persistent Now Playing pane.
        adaptiveInfo.isTwoPane -> {
            Row(Modifier.fillMaxSize()) {
                MusicNavigationRail(
                    backStack = backStack,
                    currentKey = currentKey,
                )
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            MaxContentWidth {
                                AppNavHost(
                                    backStack = backStack,
                                    viewModel = viewModel,
                                )
                            }
                        }
                    }
                }
                androidx.compose.material3.VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                ) {
                    NowPlayingPane(
                        viewModel = viewModel,
                        onOpenNowPlaying = { backStack.add(MusicRoute.NowPlaying) }
                    )
                }
            }
        }

        // Medium: navigation rail replaces the bottom bar.
        adaptiveInfo.widthClass == WindowWidthSizeClass.Medium -> {
            Row(Modifier.fillMaxSize()) {
                MusicNavigationRail(
                    backStack = backStack,
                    currentKey = currentKey,
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MaxContentWidth {
                            AppNavHost(
                                backStack = backStack,
                                viewModel = viewModel,
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = shouldShowMiniPlayer,
                            enter = slideInVertically { it } + androidx.compose.animation.fadeIn(),
                            exit = slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            MiniPlayerContainer(
                                viewModel = viewModel,
                                onClick = { backStack.add(MusicRoute.NowPlaying) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Compact: bottom navigation bar.
        else -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    MusicBottomBar(
                        backStack = backStack,
                        currentKey = currentKey,
                        shouldShowBottomBar = shouldShowBottomBar
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MaxContentWidth {
                        AppNavHost(
                            backStack = backStack,
                            viewModel = viewModel,
                        )
                    }

                    AnimatedVisibility(
                        visible = shouldShowMiniPlayer,
                        enter = slideInVertically { it } + androidx.compose.animation.fadeIn(),
                        exit = slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        MiniPlayerContainer(
                            viewModel = viewModel,
                            onClick = { backStack.add(MusicRoute.NowPlaying) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScanBootstrap(viewModel: MainViewModel) {
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    // One-shot guard: only auto-trigger the very first scan from composition.
    // Subsequent scans (e.g. after permission grant) are triggered by onResume.
    val scanAttempted = remember { mutableStateOf(false) }

    LaunchedEffect(songCount, isScanning) {
        if (songCount == 0 && !isScanning && !scanAttempted.value) {
            scanAttempted.value = true
            delay(450)
            viewModel.scanMedia()
        }
    }
}

@Composable
fun MusicBottomBar(
    backStack: NavBackStack<NavKey>,
    currentKey: MusicRoute?,
    shouldShowBottomBar: Boolean
) {
    AnimatedVisibility(
        visible = shouldShowBottomBar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 0.dp
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentKey == item.route

                NavigationBarItem(
                    icon = {
                        val icon = if (selected) item.selectedIcon else item.unselectedIcon
                        Icon(
                            imageVector = icon,
                            contentDescription = item.title
                        )
                    },
                    label = { Text(item.title) },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navigateToBottomBarTab(backStack, item.route)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun MusicNavigationRail(
    backStack: NavBackStack<NavKey>,
    currentKey: MusicRoute?
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentKey == item.route

            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navigateToBottomBarTab(backStack, item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) }
            )
        }
    }
}

@Composable
fun MiniPlayerContainer(
    viewModel: MainViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    
    val progress by remember(currentPosition, duration) {
        derivedStateOf {
            val safeDuration = duration.coerceAtLeast(0L)
            val safePosition = currentPosition.coerceAtLeast(0L)
            if (safeDuration > 0L) {
                (safePosition.coerceAtMost(safeDuration).toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
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
