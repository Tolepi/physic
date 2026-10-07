package com.physic.music

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun VerticalEqSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val hPx = with(LocalDensity.current) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val t = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val cx = size.width / 2f
            val trackW = 6.dp.toPx()
            drawRect(color.copy(alpha = 0.3f), topLeft = Offset(cx - trackW / 2f, 0f), size = Size(trackW, size.height))
            val fillH = size.height * t
            drawRect(color, topLeft = Offset(cx - trackW / 2f, size.height - fillH), size = Size(trackW, fillH))
            drawCircle(color, radius = 8.dp.toPx(), center = Offset(cx, size.height - fillH))
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val ratio = 1f - (change.position.y / hPx).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + ratio * (valueRange.endInclusive - valueRange.start))
                    }
                }
        )
    }
}

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
                Text(L.tr("play something first"), color = theme.subtext)
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
            val vertical by Prefs.eqVertical(LocalContext.current).collectAsState(initial = false)
            val numBands = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            if (vertical) {
                Row(Modifier.fillMaxWidth()) {
                    for (b in 0 until numBands) {
                        val band = b.toShort()
                        var level by remember(band) { mutableStateOf(e.getBandLevel(band).toInt()) }
                        val freqHz = e.getCenterFreq(band) / 1000
                        val freqLabel = if (freqHz < 1000) "${freqHz}Hz" else "${freqHz / 1000}kHz"
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(freqLabel, color = theme.text, fontSize = 11.sp)
                            Box(modifier = Modifier.fillMaxWidth(0.6f).height(500.dp)) {
                                VerticalEqSlider(
                                    value = level.toFloat(),
                                    valueRange = range[0].toFloat()..range[1].toFloat(),
                                    onValueChange = {
                                        level = it.toInt()
                                        e.setBandLevel(band, it.toInt().toShort())
                                    },
                                    color = theme.accent
                                )
                            }
                            Text("${level}mB", color = theme.subtext, fontSize = 11.sp)
                        }
                    }
                }
            } else {
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
}
