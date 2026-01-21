package com.suspended.music.ui.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Button
import com.suspended.music.MainActivity

class MusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceMusicWidgetContent(context)
        }
    }
}

@Composable
fun GlanceMusicWidgetContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Music Player",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        // Song info placeholder (would be updated via RemoteViews in production)
        Text(
            text = "Now Playing",
            style = TextStyle(
                color = ColorProvider(Color(0xFFBBBBBB)),
                fontSize = 12.sp
            )
        )
        
        Spacer(modifier = GlanceModifier.height(12.dp))
        
        // Control buttons
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                text = "◀",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.size(32.dp)
            )
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            Button(
                text = "⏵",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.size(40.dp)
            )
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            Button(
                text = "▶",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.size(32.dp)
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Button(
            text = "Open Player",
            onClick = actionStartActivity<MainActivity>()
        )
    }
}
