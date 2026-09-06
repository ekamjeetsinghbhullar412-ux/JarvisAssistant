package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue 
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.model.AssistantState
import com.jarvis.assistant.ui.theme.JarvisAmber
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisRed

/**
 * The glowing animated "AI core" ring — Jarvis's original, non-branded visual identity.
 * Speed and color respond to [state]; [amplitude] (0f..1f) can be driven by mic RMS
 * while listening to make the ring pulse with the user's voice.
 */
@Composable
fun AICore(
    state: AssistantState,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier.size(220.dp)
) {
    val color = when (state) {
        AssistantState.LISTENING -> JarvisCyan
        AssistantState.THINKING -> JarvisAmber
        AssistantState.SPEAKING -> JarvisCyan
        AssistantState.ERROR -> JarvisRed
        AssistantState.IDLE -> JarvisCyan.copy(alpha = 0.5f)
    }

    val rotationSpeedMs = when (state) {
        AssistantState.THINKING -> 1200
        AssistantState.LISTENING -> 2500
        else -> 6000
    }

    val infiniteTransition = rememberInfiniteTransition(label = "core")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f + (amplitude.coerceIn(0f, 1f) * 0.25f),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = (size.minDimension / 2) * 0.8f * pulse

        // Outer rotating ring, drawn as arcs to feel HUD-like rather than a plain circle.
        rotate(degrees = rotation, pivot = center) {
            for (i in 0 until 3) {
                val sweep = 70f
                val startAngle = i * 120f
                drawArc(
                    color = color.copy(alpha = 0.85f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }

        // Inner core glow
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = baseRadius * 0.65f,
            center = center
        )
        drawCircle(
            color = color,
            radius = baseRadius * 0.15f,
            center = center
        )

        // Thin static reference ring
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}
