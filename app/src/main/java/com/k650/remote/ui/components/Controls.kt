package com.k650.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k650.remote.data.PlayStatus
import com.k650.remote.data.Source
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.AmberDim

@Composable
fun VolumeControl(
    volume: Int,
    volumeMax: Int,
    muted: Boolean,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleMute) {
            Icon(
                imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (muted) "Réactiver le son" else "Couper le son",
                tint = if (muted) androidx.compose.ui.graphics.Color(0xFFE53935) else Amber,
            )
        }
        Slider(
            value = volume.toFloat(),
            onValueChange = { onPreview(it.toInt()) },
            onValueChangeFinished = { onCommit(volume) },
            valueRange = 0f..volumeMax.toFloat(),
            steps = volumeMax - 1,
            colors = SliderDefaults.colors(
                thumbColor = Amber,
                activeTrackColor = Amber,
                inactiveTrackColor = AmberDim,
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text("$volume", color = Amber, fontSize = 16.sp)
    }
}

@Composable
fun SourceSelector(
    current: Source?,
    onSelect: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Source.entries.forEach { source ->
            FilterChip(
                selected = current == source,
                onClick = { onSelect(source) },
                label = { Text(source.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Amber,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.Black,
                    labelColor = Amber,
                ),
            )
        }
    }
}

@Composable
fun TransportControls(
    playStatus: PlayStatus,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // PREVIOUS: firmware command unknown → disabled, never a no-op button.
        IconButton(onClick = {}, enabled = false) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Précédent (non supporté par le firmware)",
                tint = AmberDim,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (playStatus == PlayStatus.PLAY) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Lecture / Pause",
                tint = Amber,
                modifier = Modifier.size(44.dp),
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Morceau suivant",
                tint = Amber,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
