package com.k650.remote.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.Surface3
import com.k650.remote.ui.theme.VuHigh
import com.k650.remote.ui.theme.VuLow
import com.k650.remote.ui.theme.VuMid
import kotlin.math.abs
import kotlin.math.sin

/**
 * 90s-radio VU meter — modernised (rounded segments, soft palette).
 *
 * HONESTY CONTRACT (unchanged): the audio never passes through the phone or a
 * server and the API returns no levels, so there is NOTHING real to measure.
 * The animation is decorative and driven ONLY by data we truly have:
 *   - [active] : bars move only while playback is actually PLAY;
 *   - [level]  : overall envelope scales with the real volume 0..1.
 * The caption says so outright.
 */
@Composable
fun VuMeter(
    active: Boolean,
    level: Float,
    modifier: Modifier = Modifier,
    bars: Int = 18,
) {
    val transition = rememberInfiniteTransition(label = "vu")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val gap = size.width * 0.014f
            val barWidth = (size.width - gap * (bars - 1)) / bars
            val envelope = level.coerceIn(0f, 1f)
            val radius = CornerRadius(barWidth * 0.4f, barWidth * 0.4f)

            for (i in 0 until bars) {
                val wobble = if (active) {
                    val a = sin(phase * 1.3f + i * 0.7f)
                    val b = sin(phase * 2.1f + i * 1.9f)
                    0.5f + 0.5f * abs(a * 0.6f + b * 0.4f)
                } else 0.1f
                val h = (size.height * envelope.coerceAtLeast(0.06f) * wobble)
                    .coerceIn(size.height * 0.05f, size.height)

                val x = i * (barWidth + gap)
                drawRoundRect(
                    color = Surface3,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = barColor(h / size.height),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = radius,
                )
            }
        }
        Text(
            text = "VU-mètre décoratif — l'API de la barre ne fournit aucun niveau réel",
            style = MaterialTheme.typography.labelSmall,
            color = OnInkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

private fun barColor(fraction: Float): Color = when {
    fraction > 0.85f -> VuHigh
    fraction > 0.55f -> VuMid
    else -> VuLow
}
