package com.suspended.music.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.suspended.music.data.model.Song
import com.suspended.music.data.preferences.SettingsManager
import com.suspended.music.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicPlayer"

@UnstableApi
@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
    private val settingsManager: SettingsManager
) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()
    
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // ── Sleep timer ─────────────────────────────────────────────────────
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    /** Remaining milliseconds until the sleep timer fires, or null when inactive. */
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    /** Volume before the sleep-timer fade, so it can always be restored. */
    private var sleepTimerOriginalVolume: Float? = null

    init {
        // Apply the audio settings that live on the ExoPlayer instance itself.
        playerScope.launch {
            settingsManager.settingsFlow
                .map { it.playback.skipSilence }
                .distinctUntilChanged()
                .collect { player.setSkipSilenceEnabled(it) }
        }
        playerScope.launch {
            // ExoPlayer merges compatible tracks by default; pausing between items
            // is how gapless is disabled.
            settingsManager.settingsFlow
                .map { it.playback.gaplessPlayback }
                .distinctUntilChanged()
                .collect { player.setPauseAtEndOfMediaItems(!it) }
        }
    }

    /**
     * Invoked by [PlaybackService] when it tears down the shared player, so the
     * UI stops showing a queue/song that no longer exists.
     */
    fun onPlaybackServiceDestroyed() {
        playerScope.launch {
            _queue.value = emptyList()
            _currentSong.value = null
            _currentIndex.value = -1
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
            _playbackError.value = null
        }
    }

    /**
     * Starts a sleep timer: after [durationMinutes] playback gradually fades to
     * silence over the last [fadeSeconds] and then pauses.
     */
    fun startSleepTimer(durationMinutes: Int, fadeSeconds: Int = 30) {
        if (durationMinutes <= 0) return
        cancelSleepTimer()
        val fadeMs = (fadeSeconds.coerceAtLeast(5) * 1000L)
        val endAt = android.os.SystemClock.elapsedRealtime() + durationMinutes * 60_000L
        val originalVolume = player.volume
        sleepTimerOriginalVolume = originalVolume

        sleepTimerJob = playerScope.launch {
            try {
                while (isActive) {
                    val remaining = endAt - android.os.SystemClock.elapsedRealtime()
                    if (remaining <= 0) break
                    _sleepTimerRemainingMs.value = remaining
                    delay(1_000)
                }
                if (!isActive) return@launch
                // Fade the volume out over the final window, then pause.
                val fadeStart = android.os.SystemClock.elapsedRealtime()
                val fadeEnd = fadeStart + fadeMs
                while (isActive && android.os.SystemClock.elapsedRealtime() < fadeEnd) {
                    val elapsed = android.os.SystemClock.elapsedRealtime() - fadeStart
                    val progress = (elapsed.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
                    player.volume = originalVolume * (1f - progress)
                    delay(100)
                }
                if (!isActive) return@launch
                player.volume = 0f
                player.pause()
            } finally {
                // Always restore volume, even if the timer was cancelled mid-fade.
                player.volume = sleepTimerOriginalVolume ?: originalVolume
                sleepTimerOriginalVolume = null
                _sleepTimerRemainingMs.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
        // Guarantee the volume is restored even if the coroutine's finally hasn't run yet.
        sleepTimerOriginalVolume?.let { player.volume = it }
        sleepTimerOriginalVolume = null
    }

    // Seeking guard — after seekTo() we suppress updatePosition() for a short
    // cooldown so the slider doesn't snap back to the pre-seek position.
    @Volatile
    private var seekCooldownUntil = 0L

    private fun sanitizeDuration(rawDurationMs: Long): Long {
        return if (rawDurationMs == C.TIME_UNSET || rawDurationMs < 0L) 0L else rawDurationMs
    }

    private fun sanitizePosition(rawPositionMs: Long, maxDurationMs: Long = _duration.value): Long {
        val nonNegativePosition = rawPositionMs.coerceAtLeast(0L)
        return if (maxDurationMs > 0L) {
            nonNegativePosition.coerceAtMost(maxDurationMs)
        } else {
            nonNegativePosition
        }
    }

    private fun updateDurationFromPlayer(fallbackDurationMs: Long? = null) {
        val fallbackDuration = fallbackDurationMs?.coerceAtLeast(0L) ?: 0L
        val playerDuration = sanitizeDuration(player.duration)
        val resolvedDuration = if (playerDuration > 0L) playerDuration else fallbackDuration
        if (_duration.value != resolvedDuration) {
            _duration.value = resolvedDuration
        }
    }
    
    private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Only update if value actually changed
                if (_isPlaying.value != isPlaying) {
                    _isPlaying.value = isPlaying
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newIndex = player.currentMediaItemIndex
                // Only update if index changed
                if (_currentIndex.value != newIndex) {
                    _currentIndex.value = newIndex
                    if (_queue.value.isNotEmpty() && newIndex >= 0) {
                        val newSong = _queue.value.getOrNull(newIndex)
                        // Only update if song actually changed
                        if (_currentSong.value != newSong) {
                            _currentSong.value = newSong
                        }
                    }
                }

                val fallbackDuration = _queue.value.getOrNull(newIndex)?.duration
                updateDurationFromPlayer(fallbackDuration)
                if (_currentPosition.value != 0L) {
                    _currentPosition.value = 0L
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY, Player.STATE_BUFFERING -> {
                        updateDurationFromPlayer(_currentSong.value?.duration)
                    }
                    Player.STATE_IDLE -> {
                        if (_duration.value != 0L) {
                            _duration.value = 0L
                        }
                        if (_currentPosition.value != 0L) {
                            _currentPosition.value = 0L
                        }
                    }
                    Player.STATE_ENDED -> {
                        val endPosition = if (_duration.value > 0L) _duration.value else sanitizePosition(player.currentPosition)
                        if (_currentPosition.value != endPosition) {
                            _currentPosition.value = endPosition
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val normalizedPosition = sanitizePosition(newPosition.positionMs)
                if (_currentPosition.value != normalizedPosition) {
                    _currentPosition.value = normalizedPosition
                }
                updateDurationFromPlayer(_currentSong.value?.duration)
            }
            
            override fun onRepeatModeChanged(repeatMode: Int) {
                // Only update if mode actually changed
                if (_repeatMode.value != repeatMode) {
                    _repeatMode.value = repeatMode
                }
            }
            
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                // Only update if shuffle state changed
                if (_shuffleEnabled.value != shuffleModeEnabled) {
                    _shuffleEnabled.value = shuffleModeEnabled
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                val speed = playbackParameters.speed
                if (_playbackSpeed.value != speed) {
                    _playbackSpeed.value = speed
                }
            }
            
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0 && _audioSessionId.value != audioSessionId) {
                    _audioSessionId.value = audioSessionId
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                ErrorHandler.handlePlaybackError(error)
                _playbackError.value = ErrorHandler.playbackErrorText(error)
                super.onPlayerError(error)
            }
        }
    
    private val audioDeviceCallback = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>) {
            super.onAudioDevicesAdded(addedDevices)
            if (settingsManager.settingsFlow.value.playback.autoResume) {
                // Resume if wired headset or bluetooth A2DP is connected
                val shouldResume = addedDevices.any { device ->
                    device.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }
                
                if (shouldResume && !player.isPlaying && player.playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Headset connected, auto-resuming playback")
                    player.play()
                }
            }
        }
    }

    init {
        player.addListener(playerListener)
        
        // Ensure playback speed is set to 1.0x (normal speed)
        player.setPlaybackParameters(player.playbackParameters.withSpeed(1.0f))
        
        // Register headset plug detection
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }
    
    fun playSong(song: Song) {
        _playbackError.value = null
        _currentPosition.value = 0L
        _duration.value = song.duration.coerceAtLeast(0L)
        _currentSong.value = song
        _queue.value = listOf(song)
        _currentIndex.value = 0

        val mediaItem = song.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Update Session ID immediately if available
        if (player.audioSessionId != 0) {
            _audioSessionId.value = player.audioSessionId
        }
    }
    
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) {
            Log.w(TAG, "playSongs called with empty song list")
            return
        }
        
        com.suspended.music.util.PerfLogger.startTrace("PlaySongs")
        Log.d(TAG, "Playing ${songs.size} songs, starting at index $startIndex")
        
        _playbackError.value = null
        _currentPosition.value = 0L
        _duration.value = songs.getOrNull(startIndex)?.duration?.coerceAtLeast(0L) ?: 0L
        _queue.value = songs
        _currentSong.value = songs.getOrNull(startIndex)
        _currentIndex.value = startIndex

        val mediaItems = songs.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        // Preserve user-set playback speed (no forced reset to 1.0x)
        player.play()
        com.suspended.music.util.PerfLogger.endTrace("PlaySongs")
    }
    
    fun play() {
        player.play()
    }
    
    fun pause() {
        player.pause()
    }
    
    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }
    
    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }
    
    fun skipToPrevious() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }
    
    fun seekTo(position: Long) {
        val targetPosition = if (_duration.value > 0L) {
            position.coerceIn(0L, _duration.value)
        } else {
            position.coerceAtLeast(0L)
        }

        // Set cooldown BEFORE seeking so updatePosition() ignores stale reads
        seekCooldownUntil = System.currentTimeMillis() + 600L
        _currentPosition.value = targetPosition
        player.seekTo(targetPosition)
    }
    
    fun toggleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    
    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }
    
    fun setPlaybackSpeed(speed: Float) {
        if (_playbackSpeed.value == speed) return
        Log.d(TAG, "Setting playback speed to ${String.format("%.2f", speed)}x")
        player.setPlaybackParameters(player.playbackParameters.withSpeed(speed))
        _playbackSpeed.value = speed
    }
    
    fun updatePosition() {
        // While cooling down after a seek, skip updates so the slider doesn't snap
        if (System.currentTimeMillis() < seekCooldownUntil) return
        if (!player.isPlaying) return

        updateDurationFromPlayer(_currentSong.value?.duration)
 
        val newPosition = sanitizePosition(player.currentPosition)
        // Only update if position changed by at least 250ms (avoid micro-jitter)
        if (kotlin.math.abs(newPosition - _currentPosition.value) >= 250) {
            _currentPosition.value = newPosition
        }
    }

    fun refreshPositionSnapshot() {
        updateDurationFromPlayer(_currentSong.value?.duration)

        val latestPosition = sanitizePosition(player.currentPosition)
        if (_currentPosition.value != latestPosition) {
            _currentPosition.value = latestPosition
        }
    }
    
    fun addToQueue(song: Song) {
        val mediaItem = song.toMediaItem()
        player.addMediaItem(mediaItem)
        _queue.value = _queue.value + song
    }
    
    fun addToQueue(songs: List<Song>) {
        val mediaItems = songs.map { it.toMediaItem() }
        player.addMediaItems(mediaItems)
        _queue.value = _queue.value + songs
    }
    
    fun removeFromQueue(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
            // Adjust current index if removing item before or at the current position
            val ci = _currentIndex.value
            if (index < ci) {
                _currentIndex.value = ci - 1
            } else if (index == ci && _queue.value.isNotEmpty()) {
                // Current song was removed; update to whatever ExoPlayer chose
                val newIndex = player.currentMediaItemIndex.coerceIn(0, _queue.value.lastIndex)
                _currentIndex.value = newIndex
                _currentSong.value = _queue.value.getOrNull(newIndex)
            }
            // If queue is now empty, clear state
            if (_queue.value.isEmpty()) {
                _currentSong.value = null
                _currentIndex.value = -1
            }
        }
    }
    
    fun clearQueue() {
        player.clearMediaItems()
        _queue.value = emptyList()
        _currentSong.value = null
        _currentIndex.value = -1
    }
    
    fun skipToIndex(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0)
        }
    }
    
    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setDurationMs(duration.coerceAtLeast(0L))
            .build()
 
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(uri))
            // Declare the format explicitly: content sniffing often fails for
            // AAC/M4A/Opus/OGG, which would prevent both extraction and the
            // FFmpeg fallback renderer from ever being selected.
            .setMimeType(mimeType)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Releases all player resources to prevent memory leaks
     */
    fun release() {
        cancelSleepTimer()
        playerScope.cancel()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        player.removeListener(playerListener)
        player.stop()
        player.clearMediaItems()
        player.release()
    }
}
