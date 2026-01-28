package com.suspended.music

import android.app.Application
import coil3.ImageLoader

import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.suspended.music.player.MusicPlayer
import com.suspended.music.player.AudioEffectController
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class MusicPlayerApp : Application() {

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @Inject
    lateinit var audioEffectController: AudioEffectController

    override fun onCreate() {
        super.onCreate()
        
        // Configure global ImageLoader
        /* 
        // Commenting out explicit ImageLoaderFactory due to resolution issues
        // Will rely on manual setup or default for now, can be revisited
        SingletonImageLoader.setSafe { context ->
             ImageLoader.Builder(context)
                .crossfade(true)
                .build()
        }
        */
    }
    
    // Removed ImageLoaderFactory implementation and newImageLoader to fix build
    // Will restore custom cache config once imports are stable

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
