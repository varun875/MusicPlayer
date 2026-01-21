package com.suspended.music.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suspended.music.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicPlayer"

@Singleton
class MusicPlayer @Inject constructor(
    private val player: ExoPlayer
) {
    
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
    
    init {
        player.addListener(object : Player.Listener {
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
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val newDuration = player.duration
                    // Only update if duration changed
                    if (_duration.value != newDuration && newDuration > 0) {
                        _duration.value = newDuration
                    }
                }
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
            
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0 && _audioSessionId.value != audioSessionId) {
                    _audioSessionId.value = audioSessionId
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}", error)
                Log.e(TAG, "Error code: ${error.errorCodeName}")
                if (error.cause != null) {
                    Log.e(TAG, "Root cause: ${error.cause?.message}")
                }
                _playbackError.value = "Playback Error: ${error.errorCodeName} - ${error.message}"
                super.onPlayerError(error)
            }
        })
        
        // Ensure playback speed is set to 1.0x (normal speed)
        player.setPlaybackParameters(player.playbackParameters.withSpeed(1.0f))
    }
    
    fun playSong(song: Song) {
        val mediaItem = song.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _currentSong.value = song
        _queue.value = listOf(song)
        _currentIndex.value = 0
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
        
        Log.d(TAG, "Playing ${songs.size} songs, starting at index $startIndex")
        val startSong = songs[startIndex]
        Log.d(TAG, "First song: ${startSong.title} - ${startSong.uri}")
        
        // Check if it's FLAC and log for debugging
        if (startSong.path.endsWith(".flac", ignoreCase = true)) {
            Log.d(TAG, "Playing FLAC file: ${startSong.path}")
        }
        
        _playbackError.value = null
        val mediaItems = songs.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        // Ensure playback speed is 1.0x before playing
        player.setPlaybackParameters(player.playbackParameters.withSpeed(1.0f))
        player.play()
        _queue.value = songs
        _currentSong.value = songs.getOrNull(startIndex)
        _currentIndex.value = startIndex
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
        player.seekTo(position)
        _currentPosition.value = position
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
        Log.d(TAG, "Setting playback speed to ${String.format("%.2f", speed)}x")
        player.setPlaybackParameters(player.playbackParameters.withSpeed(speed))
    }
    
    fun updatePosition() {
        val newPosition = player.currentPosition
        // Only update if position changed significantly (avoid micro-updates)
        if (kotlin.math.abs(newPosition - _currentPosition.value) >= 100) {
            _currentPosition.value = newPosition
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
            .build()
        
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }
}
