package com.k650.remote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.AmberGlow
import com.k650.remote.ui.theme.Bad

/**
 * Amber dot-matrix panel echoing the HW-K650 front display: source on the left,
 * volume on the right, over a faint LED dot grid. Grid decorative; text is real.
 */
@Composable
fun MatrixDisplay(
    source: String,
    volume: Int,
    volumeMax: Int,
    muted: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF120E06), Color(0xFF0A0803))))
            .border(1.dp, AmberGlow, RoundedCornerShape(18.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
            val step = 9.dp.toPx()
            val r = 0.8.dp.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(color = AmberGlow, radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = source.uppercase(),
                color = Amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = if (muted) "— MUTE —" else "VOL ${"%02d".format(volume)} / $volumeMax",
                color = if (muted) Bad else Amber.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
