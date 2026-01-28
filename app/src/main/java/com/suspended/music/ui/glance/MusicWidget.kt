package com.suspended.music.ui.glance

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.suspended.music.MainActivity
import com.suspended.music.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private val titleKey = stringPreferencesKey("widget_title")
private val artistKey = stringPreferencesKey("widget_artist")
private val isPlayingKey = booleanPreferencesKey("widget_playing")
private val hasPrevKey = booleanPreferencesKey("widget_has_prev")
private val hasNextKey = booleanPreferencesKey("widget_has_next")
private val durationKey = longPreferencesKey("widget_duration")
private val positionKey = longPreferencesKey("widget_position")

class MusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceMusicWidgetContent()
        }
    }

    companion object {
        suspend fun updateFromController(context: Context, controller: MediaController) {
            val metadata = controller.mediaMetadata
            val title = metadata.title?.toString()
            val artist = metadata.artist?.toString()
            val isPlaying = controller.isPlaying
            val hasPrev = controller.hasPreviousMediaItem()
            val hasNext = controller.hasNextMediaItem()
            val duration = controller.duration.coerceAtLeast(0)
            val position = controller.currentPosition.coerceAtLeast(0)
            updateWidgetState(context, title, artist, isPlaying, hasPrev, hasNext, duration, position)
        }

        suspend fun updateWidgetState(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            hasPrev: Boolean,
            hasNext: Boolean,
            duration: Long,
            position: Long
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MusicWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        writeState(this, title, artist, isPlaying, hasPrev, hasNext, duration, position)
                    }
                }
            }
            MusicWidget().updateAll(context)
        }

        private fun writeState(
            prefs: MutablePreferences,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            hasPrev: Boolean,
            hasNext: Boolean,
            duration: Long,
            position: Long
        ) {
            prefs[titleKey] = title ?: "Not playing"
            prefs[artistKey] = artist ?: ""
            prefs[isPlayingKey] = isPlaying
            prefs[hasPrevKey] = hasPrev
            prefs[hasNextKey] = hasNext
            prefs[durationKey] = duration
            prefs[positionKey] = position
        }
    }
}

@Composable
private fun GlanceMusicWidgetContent() {
    val prefs = currentState<Preferences>()
    val title = prefs[titleKey] ?: "Not playing"
    val artist = prefs[artistKey] ?: ""
    val isPlaying = prefs[isPlayingKey] ?: false
    val hasPrev = prefs[hasPrevKey] ?: false
    val hasNext = prefs[hasNextKey] ?: false
    val duration = prefs[durationKey] ?: 1L
    val position = prefs[positionKey] ?: 0L
    
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1A1A1A)))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "♫ NOW PLAYING",
            style = TextStyle(
                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9D00FF)),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Song Title
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )

        // Artist Name
        if (artist.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = artist,
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFBBBBBB)),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        // Progress Bar (Visual only, snapshot)
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp),
            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9D00FF)),
            backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF333333))
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Control Buttons
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                text = "⏮",
                enabled = hasPrev,
                onClick = actionRunCallback<PrevAction>()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Button(
                text = if (isPlaying) "⏸" else "▶",
                onClick = actionRunCallback<PlayPauseAction>()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Button(
                text = "⏭",
                enabled = hasNext,
                onClick = actionRunCallback<NextAction>()
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Footer hint
        Text(
            text = "Tap widget to open player",
            style = TextStyle(
                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF666666)),
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
    }
}

private suspend fun buildController(context: Context): MediaController? = withContext(Dispatchers.IO) {
    runCatching {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().get(5, TimeUnit.SECONDS)
    }.getOrNull()
}

private suspend inline fun withController(
    context: Context,
    crossinline block: suspend MediaController.() -> Unit
) {
    withContext(Dispatchers.IO) {
        val controller = buildController(context) ?: return@withContext
        runCatching { 
            controller.block()
            // Give the command time to execute
            kotlinx.coroutines.delay(100)
        }
        // Update widget state after command
        withContext(Dispatchers.Main) {
            MusicWidget.updateFromController(context, controller)
        }
        controller.release()
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) {
            if (isPlaying) pause() else play()
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { seekToNextMediaItem() }
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) {
            if (currentPosition > 3000) {
                seekTo(0)
            } else {
                seekToPreviousMediaItem()
            }
        }
    }
}
