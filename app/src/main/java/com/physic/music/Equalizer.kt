package com.physic.music

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EqualizerTab(theme: ThemeColors) {
    val player = Playback.player
    var eq by remember { mutableStateOf<Equalizer?>(null) }
    var enabled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { eq?.release() } }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Panel("equalizer", theme, shape = RoundedCornerShape(0.dp)) {
            if (player == null) {
                Text("play something first", color = theme.subtext)
                return@Panel
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (enabled) "on" else "off", color = theme.text, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { on ->
                    enabled = on
                    try {
                        if (on) {
                            val e = Equalizer(0, player.audioSessionId)
                            e.enabled = true
                            eq = e
                        } else {
                            eq?.enabled = false
                            eq?.release()
                            eq = null
                        }
                        error = null
                    } catch (ex: Exception) { error = ex.message; enabled = false }
                })
            }
            if (error != null) Text(error!!, color = theme.accent, fontSize = 12.sp)
        }

        eq?.let { e ->
            val numBands = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            for (b in 0 until numBands) {
                val band = b.toShort()
                var level by remember(band) { mutableStateOf(e.getBandLevel(band).toInt()) }
                val freqHz = e.getCenterFreq(band) / 1000
                val freqLabel = if (freqHz < 1000) "${freqHz}Hz" else "${freqHz / 1000}kHz"
                Column {
                    Text("$freqLabel   ${level}mB", color = theme.text, fontSize = 12.sp)
                    Slider(
                        value = level.toFloat(),
                        valueRange = range[0].toFloat()..range[1].toFloat(),
                        onValueChange = {
                            level = it.toInt()
                            e.setBandLevel(band, it.toInt().toShort())
                        },
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                    )
                }
            }
        }
    }
}
