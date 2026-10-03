package com.physic.music

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TogglePlayback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        context.startService(Intent(context, MusicService::class.java).setAction("TOGGLE"))
    }
}

class NextTrack : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        context.startService(Intent(context, MusicService::class.java).setAction("NEXT"))
    }
}

class MusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val title = Playback.title.value.ifEmpty { "Not playing" }
        val artist = Playback.artist.value
        val playing = Playback.playing.value
        provideContent {
            Column {
                Text(title, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Text(artist, maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xFF9E9E9E)), fontSize = 12.sp))
                Row {
                    Text(if (playing) "❚❚" else "▶", style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp),
                        modifier = GlanceModifier.clickable(actionRunCallback<TogglePlayback>()))
                    Text(L.tr("    "), style = TextStyle(fontSize = 20.sp))
                    Text(L.tr("⏭"), style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp),
                        modifier = GlanceModifier.clickable(actionRunCallback<NextTrack>()))
                }
            }
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MusicWidget()
}
