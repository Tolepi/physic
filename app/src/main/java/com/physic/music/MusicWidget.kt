package com.physic.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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

class PrevTrack : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        context.startService(Intent(context, MusicService::class.java).setAction("PREV"))
    }
}

class MusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val title = Playback.title.value.ifEmpty { "Not playing" }
        val artist = Playback.artist.value
        val playing = Playback.playing.value
        val albumArtBitmap = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: 0)?.let { song ->
            val albumArtUri = android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                song.albumId
            )
            try {
                val stream = context.contentResolver.openInputStream(albumArtUri)
                val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                stream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
        }
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
                    if (albumArtBitmap != null) {
                        Image(
                            provider = ImageProvider(albumArtBitmap),
                            contentDescription = "Album art",
                            modifier = GlanceModifier.size(48.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth().width(160.dp)) {
                        Text(
                            title,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            artist,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF9E9E9E)),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "\u23EE",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp),
                        modifier = GlanceModifier.clickable(actionRunCallback<PrevTrack>())
                    )
                    Spacer(modifier = GlanceModifier.width(24.dp))
                    Text(
                        if (playing) "\u23F8" else "\u25B6",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 28.sp),
                        modifier = GlanceModifier.clickable(actionRunCallback<TogglePlayback>())
                    )
                    Spacer(modifier = GlanceModifier.width(24.dp))
                    Text(
                        "\u23ED",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp),
                        modifier = GlanceModifier.clickable(actionRunCallback<NextTrack>())
                    )
                }
            }
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MusicWidget()
}