package com.k650.remote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.AmberDim

/**
 * Amber dot-matrix panel echoing the HW-K650 front display: source on the left,
 * volume on the right, over a faint LED dot grid. The grid is decorative; the
 * text is the real state.
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
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B0906))
            .border(1.dp, AmberDim, RoundedCornerShape(10.dp)),
    ) {
        // Faint LED dot grid.
        Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
            val step = 8.dp.toPx()
            val r = 0.7.dp.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(color = AmberDim, radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                text = source.uppercase(),
                color = Amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                text = if (muted) "MUTE" else "VOL ${"%2d".format(volume)} / $volumeMax",
                color = if (muted) Color(0xFFE53935) else Amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
