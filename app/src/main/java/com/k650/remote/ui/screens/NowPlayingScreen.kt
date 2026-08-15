package com.k650.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import com.k650.remote.data.PlayStatus
import com.k650.remote.data.RepeatMode
import com.k650.remote.data.SoundbarState
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.components.GlassCard
import com.k650.remote.ui.components.MatrixDisplay
import com.k650.remote.ui.components.VuMeter
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.OnInkMed
import com.k650.remote.ui.theme.Outline

@Composable
fun NowPlayingScreen(vm: SoundbarViewModel) {
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(
            title = state.speakerName ?: "Samsung Soundbar K650",
            reachable = state.reachable,
            onRefresh = vm::refreshNow,
        )

        MatrixDisplay(
            source = state.source?.label ?: "—",
            volume = state.volume,
            volumeMax = SoundbarState.VOLUME_MAX,
            muted = state.muted,
        )

        NowPlayingCard(state, vm)

        VuMeter(
            active = state.nowPlaying.playStatus == PlayStatus.PLAY,
            level = state.volume.toFloat() / SoundbarState.VOLUME_MAX,
        )

        VolumeRow(state, vm)
    }
}

@Composable
private fun ScreenTitle(title: String, reachable: Boolean, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(9.dp).clip(CircleShape)
                .background(if (reachable) Color(0xFF57C77D) else Color(0xFFE5534B))
        )
        Spacer(Modifier.size(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NowPlayingCard(state: SoundbarState, vm: SoundbarViewModel) {
    val np = state.nowPlaying
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, Amber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B0906)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!np.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = np.thumbnailUrl,
                            contentDescription = "Pochette",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                        )
                    } else {
                        Text("♪", color = Amber, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        np.title?.takeIf { it.isNotBlank() } ?: "Aucune lecture",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (!np.artist.isNullOrBlank()) {
                        Text(np.artist, color = OnInkMed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!np.cpName.isNullOrBlank()) {
                        Text(np.cpName, color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            ProgressRow(state, vm)
            Spacer(Modifier.height(8.dp))
            TransportRow(state, vm)
        }
    }
}

@Composable
private fun ProgressRow(state: SoundbarState, vm: SoundbarViewModel) {
    val pos = state.position
    val len = pos.lengthSec
    // Local drag override so the thumb tracks the finger; cleared on release.
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    val shown = if (dragging) dragFraction else pos.fraction
    Column {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragFraction = it },
            onValueChangeFinished = {
                if (len > 0) vm.seek((dragFraction * len).toInt())
                dragging = false
            },
            enabled = len > 0,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Amber, activeTrackColor = Amber, inactiveTrackColor = Outline,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shownSec = if (dragging && len > 0) (dragFraction * len).toInt() else pos.positionSec
            Text(formatTime(shownSec), color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
            Text(if (len > 0) formatTime(len) else "--:--", color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TransportRow(state: SoundbarState, vm: SoundbarViewModel) {
    val playing = state.nowPlaying.playStatus == PlayStatus.PLAY
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::cycleRepeat) {
            Icon(
                imageVector = if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Répéter",
                tint = if (state.repeat == RepeatMode.OFF) OnInkFaint else Amber,
            )
        }
        IconButton(onClick = vm::previous) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Précédent (redémarre le morceau)", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
        }
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape).background(Amber),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = vm::togglePlayPause) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Lecture / Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        IconButton(onClick = vm::skipNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Suivant", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = vm::toggleShuffle) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Aléatoire", tint = if (state.shuffle) Amber else OnInkFaint)
        }
    }
}

@Composable
private fun VolumeRow(state: SoundbarState, vm: SoundbarViewModel) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = vm::toggleMute) {
                Icon(
                    imageVector = if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Muet",
                    tint = if (state.muted) Color(0xFFE5534B) else Amber,
                )
            }
            Slider(
                value = state.volume.toFloat(),
                onValueChange = { vm.previewVolume(it.toInt()) },
                onValueChangeFinished = { vm.commitVolume(state.volume) },
                valueRange = 0f..SoundbarState.VOLUME_MAX.toFloat(),
                steps = SoundbarState.VOLUME_MAX - 1,
                colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber, inactiveTrackColor = Outline),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("${state.volume}", color = Amber, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatTime(sec: Int): String {
    if (sec <= 0) return "0:00"
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
