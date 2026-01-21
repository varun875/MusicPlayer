package com.suspended.music.di

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import coil.ImageLoader
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }
    
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideRenderersFactory(
        @ApplicationContext context: Context
    ): DefaultRenderersFactory {
        // Block buggy hardware FLAC decoder and prefer software alternatives
        val softwareFirstSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunneledDecoder ->
            try {
                val allDecoders = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunneledDecoder)
                
                // Explicitly filter out problematic c2.android.flac.decoder
                val filtered = allDecoders.filterNot { 
                    it.name.contains("c2.android.flac.decoder", ignoreCase = true)
                }
                
                // Log available decoders for FLAC to diagnose selection
                if (mimeType.contains("flac", ignoreCase = true)) {
                    Log.d("PlayerModule", "FLAC decoders available: ${allDecoders.map { "${it.name} (hw:${it.hardwareAccelerated}, sw:${it.softwareOnly})" }}")
                    Log.d("PlayerModule", "After filtering: ${filtered.map { it.name }}")
                }
                
                // Return sorted list: software-only first, then non-hardware-accelerated
                return@MediaCodecSelector filtered.sortedWith(
                    compareByDescending<MediaCodecInfo> { it.softwareOnly }
                        .thenBy { it.hardwareAccelerated }
                )
            } catch (e: Exception) {
                Log.e("PlayerModule", "MediaCodecSelector error: ${e.message}")
                return@MediaCodecSelector emptyList()
            }
        }

        return DefaultRenderersFactory(context).apply {
            // Disable extension renderers - they have buffer management issues
            // Use software decoders which are more stable
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            // Disable float output to avoid timestamp discontinuity with software FLAC decoder
            setEnableAudioFloatOutput(false)
            setMediaCodecSelector(softwareFirstSelector)
            // Allow fallback to alternative decoders when the preferred one fails
            setEnableDecoderFallback(true)
        }
    }
    
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        renderersFactory: DefaultRenderersFactory
    ): ExoPlayer {
        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(100) // Fast crossfade
            .respectCacheHeaders(false) // Don't revalidate cached images
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
