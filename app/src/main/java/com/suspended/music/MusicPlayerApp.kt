package com.suspended.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicPlayerApp : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35) // Increased from 0.25 for better in-app caching
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // Increased from 0.02 for persistent caching
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
