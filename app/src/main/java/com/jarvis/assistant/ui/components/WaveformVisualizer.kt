package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.JarvisCyan
import kotlin.math.sin
import kotlin.random.Random

/**
 * Simple animated bar-style waveform. [active] controls whether it animates;
 * purely decorative/status feedback (does not require raw audio buffer access).
 */
@Composable
fun WaveformVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (active) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 24
        val barWidth = size.width / (barCount * 2)
        for (i in 0 until barCount) {
            val seed = Random(i).nextFloat()
            val heightFactor = if (active) {
                (0.3f + 0.7f * kotlin.math.abs(sin(phase + i * 0.5f + seed)))
            } else 0.08f
            val barHeight = size.height * heightFactor
            drawLine(
                color = JarvisCyan.copy(alpha = if (active) 0.9f else 0.3f),
                start = androidx.compose.ui.geometry.Offset(i * barWidth * 2 + barWidth / 2, size.height / 2 - barHeight / 2),
                end = androidx.compose.ui.geometry.Offset(i * barWidth * 2 + barWidth / 2, size.height / 2 + barHeight / 2),
                strokeWidth = barWidth * 0.8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
