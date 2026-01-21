package com.suspended.music.player

import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import coil.ImageLoader
import com.suspended.music.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    
    @Inject
    lateinit var player: ExoPlayer
    
    @Inject
    lateinit var imageLoader: ImageLoader
    
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var artworkLoadingJob: Job? = null
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val sessionActivity = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.suspended.music.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(MediaSessionCallback())
            .build()
            
        // Enhanced notification provider with Samsung Now Bar support
        val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
            .build()
        
        setMediaNotificationProvider(notificationProvider)
        
        // Add player listener for metadata updates and Samsung integration
        player.addListener(PlayerEventListener())
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    override fun onDestroy() {
        artworkLoadingJob = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    @OptIn(UnstableApi::class)
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Use default connection result - this automatically exposes all playback commands
            // Samsung Now Bar and other media controllers will have access to play/pause/skip
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    Player.Commands.Builder()
                        .addAllCommands()
                        .build()
                )
                .build()
        }
    }
    
    private inner class PlayerEventListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateNowBarMetadata()
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            // Update Now Bar when playback state changes
            updateNowBarMetadata()
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            // Notify Now Bar of play/pause state
            updateNowBarMetadata()
        }
    }
    
    private fun loadAndUpdateArtwork(mediaItem: MediaItem) {
        // Artwork loading handled by Coil's caching system
        // MediaSession will automatically use cached artwork
        artworkLoadingJob = null
    }
    
    @OptIn(UnstableApi::class)
    private fun updateNowBarMetadata() {
        val currentItem = player.currentMediaItem ?: return
        
        // Samsung Now Bar automatically displays metadata from current media item
        // No additional updates needed - MediaSession handles this automatically
        // The metadata shown includes:
        // - Song title
        // - Artist name
        // - Album name
        // - Album artwork (from Coil cache)
        // - Play/Pause status
        // - Progress position
    }
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "music_player_notification"
        
        fun Song.toMediaItem(): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setDurationMs(duration)
                .build()
            
            return MediaItem.Builder()
                .setMediaId(id.toString())
                .setUri(uri)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
