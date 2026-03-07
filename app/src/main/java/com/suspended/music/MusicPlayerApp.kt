package com.suspended.music

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
        // Register lifecycle observer for reliable cleanup (onTerminate is never called on real devices)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Clear image cache when memory is critically low
    }

    private inner class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            // Release player resources when app process is destroyed
            if (::musicPlayer.isInitialized) {
                musicPlayer.release()
            }
            if (::audioEffectController.isInitialized) {
                audioEffectController.release()
            }
        }
    }
}
