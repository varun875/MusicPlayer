package com.suspended.music

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.suspended.music.ui.utils.BatteryOptimizationManager
import com.suspended.music.util.PerfLogger
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class MusicPlayerApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * The Coil singleton, captured when [newImageLoader] builds it so we can
     * shed its in-memory cache under memory pressure. Volatile because it's
     * created lazily on whichever thread first touches the loader, but read
     * from onTrimMemory/onLowMemory on the main thread.
     */
    @Volatile
    private var imageLoader: ImageLoader? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Trace logging is a debug aid; never ship it enabled in release.
        PerfLogger.enabled = BuildConfig.DEBUG
        // Register the power-save/battery listener (otherwise the capability is inert).
        BatteryOptimizationManager.initialize(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .build()
            .also { imageLoader = it }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Only shed the image cache under critical memory pressure so normal
        // backgrounding doesn't force re-decoding every album thumbnail on return.
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            imageLoader?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Last chance before the process may be killed: drop it all.
        imageLoader?.memoryCache?.clear()
    }
}
