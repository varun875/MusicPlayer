package com.suspended.music.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suspended.music.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    
    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var musicPlayer: MusicPlayer

    private var mediaSession: MediaSession? = null
    
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
            .build()

        // Enhanced notification provider with Samsung Now Bar support
        val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
            .build()
        
        setMediaNotificationProvider(notificationProvider)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        // Keep the service alive while a queue exists so playback can resume;
        // only tear down when there is nothing left to play.
        if (player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    // Note: MediaSessionService has no documented onNotificationDeleted
    // override; by default it stops itself when the notification is dismissed.
    // onTaskRemoved above guards the app-switcher case, and
    // onPlaybackServiceDestroyed resets the UI mirror on actual teardown.

    override fun onDestroy() {
        mediaSession?.run {
            // Don't release the ExoPlayer here — it's a Hilt singleton shared with MusicPlayer.
            // Releasing it would cause a double-release crash.
            player.stop()
            player.clearMediaItems()
            release() // Release MediaSession only
            mediaSession = null
        }
        // The shared player was just cleared, so reset MusicPlayer's mirror state
        // too — otherwise the UI keeps showing a queue that no longer exists.
        musicPlayer.onPlaybackServiceDestroyed()
        super.onDestroy()
    }
}
