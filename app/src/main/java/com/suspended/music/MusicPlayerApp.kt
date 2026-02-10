package com.suspended.music

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okio.Path.Companion.toOkioPath
import com.suspended.music.player.MusicPlayer
import com.suspended.music.player.AudioEffectController

@UnstableApi
@HiltAndroidApp
class MusicPlayerApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @Inject
    lateinit var audioEffectController: AudioEffectController

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Clear image memory cache when memory is low
        // ImageLoaderFactory manages the singleton instance
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Clear image cache when memory is critically low
        // ImageLoaderFactory manages the singleton instance
    }

    override fun onTerminate() {
        super.onTerminate()
        // Release player resources when app terminates
        if (::musicPlayer.isInitialized) {
            musicPlayer.release()
        }
        if (::audioEffectController.isInitialized) {
            audioEffectController.release()
        }
    }
}
