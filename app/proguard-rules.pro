# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Fix for R8 Kotlin metadata parsing errors with Kotlin 2.1.10
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Suppress warnings about Kotlin metadata
-dontwarn kotlin.Metadata

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep for jaudiotagger (tag editing library)
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn javax.imageio.**
-dontwarn java.awt.image.**

# Fix for R8 missing class errors with errorprone annotations
-dontwarn javax.lang.model.**
-dontwarn javax.lang.model.element.Modifier
-dontwarn com.google.errorprone.annotations.**

# FFmpeg software decoder (media3-ffmpeg-decoder / Jellyfin prebuilt).
# DefaultRenderersFactory instantiates FfmpegAudioRenderer via reflection
# (Class.forName), so R8 must not strip or rename these classes in release.
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
  private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}
-dontwarn androidx.media3.decoder.ffmpeg.**
-keepclasseswithmembernames class * {
  native <methods>;
}