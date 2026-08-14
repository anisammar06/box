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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.AmberDim
import kotlin.math.abs
import kotlin.math.sin

/**
 * 90s-radio VU meter.
 *
 * HONESTY CONTRACT (from the brief): the audio never passes through the phone
 * or a server, and the API returns no levels — so there is NOTHING real to
 * measure. This animation is purely decorative and is driven ONLY by data we
 * genuinely have:
 *   - [active]  : bars move only while playback is actually PLAY;
 *   - [level]   : the overall envelope height scales with the real volume 0..1.
 * A caption states outright that it is decorative.
 */
@Composable
fun VuMeter(
    active: Boolean,
    level: Float,
    modifier: Modifier = Modifier,
    bars: Int = 16,
) {
    val transition = rememberInfiniteTransition(label = "vu")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val gap = size.width * 0.012f
            val barWidth = (size.width - gap * (bars - 1)) / bars
            val envelope = level.coerceIn(0f, 1f)

            for (i in 0 until bars) {
                // Deterministic pseudo-motion: layered sines per bar index. When
                // inactive, bars fall to a flat idle floor scaled by volume.
                val wobble = if (active) {
                    val a = sin(phase * 1.3f + i * 0.7f)
                    val b = sin(phase * 2.1f + i * 1.9f)
                    (0.55f + 0.45f * abs(a * 0.6f + b * 0.4f))
                } else {
                    0.12f
                }
                val h = (size.height * envelope.coerceAtLeast(0.05f) * wobble)
                    .coerceIn(size.height * 0.04f, size.height)

                val x = i * (barWidth + gap)
                // dim base column
                drawRect(
                    color = AmberDim,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                )
                // lit portion, green→amber→red gradient by height like a real VU
                drawRect(
                    color = barColor(h / size.height),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                )
            }
        }
        Text(
            text = "VU-mètre décoratif — l'API ne fournit aucun niveau réel",
            color = Amber.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

private fun barColor(fraction: Float): Color = when {
    fraction > 0.85f -> Color(0xFFE53935) // red peaks
    fraction > 0.6f -> Color(0xFFFFB000)  // amber
    else -> Color(0xFF7CB342)             // green
}
