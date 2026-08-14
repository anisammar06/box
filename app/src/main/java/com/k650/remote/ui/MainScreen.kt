package com.k650.remote.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.k650.remote.data.MusicService
import com.k650.remote.data.PlayStatus
import com.k650.remote.data.Source
import com.k650.remote.ui.components.MatrixDisplay
import com.k650.remote.ui.components.SourceSelector
import com.k650.remote.ui.components.TransportControls
import com.k650.remote.ui.components.VolumeControl
import com.k650.remote.ui.components.VuMeter
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.AmberDim
import com.k650.remote.ui.theme.PanelSurface

@Composable
fun MainScreen(vm: SoundbarViewModel) {
    val state by vm.state.collectAsState()
    val snackText by vm.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettings by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackText) {
        snackText?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(
                name = state.speakerName ?: "Samsung Soundbar K650",
                reachable = state.reachable,
                onRefresh = vm::refreshNow,
                onSettings = { showSettings = true },
            )

            if (!state.reachable) {
                UnreachableBanner(state.lastError)
            }

            MatrixDisplay(
                source = state.source?.label ?: "—",
                volume = state.volume,
                volumeMax = com.k650.remote.data.SoundbarState.VOLUME_MAX,
                muted = state.muted,
            )

            NowPlayingCard(state)

            VuMeter(
                active = state.nowPlaying.playStatus == PlayStatus.PLAY,
                level = state.volume.toFloat() / com.k650.remote.data.SoundbarState.VOLUME_MAX,
            )

            TransportControls(
                playStatus = state.nowPlaying.playStatus,
                onPlayPause = vm::togglePlayPause,
                onNext = vm::skipNext,
            )

            VolumeControl(
                volume = state.volume,
                volumeMax = com.k650.remote.data.SoundbarState.VOLUME_MAX,
                muted = state.muted,
                onPreview = vm::previewVolume,
                onCommit = vm::commitVolume,
                onToggleMute = vm::toggleMute,
            )

            SectionLabel("Source d'entrée")
            SourceSelector(current = state.source, onSelect = vm::selectSource)

            SectionLabel("Raccourcis")
            ShortcutsRow(
                onBluetooth = vm::switchToBluetooth,
                onPlayUrl = { showUrlDialog = true },
            )

            SectionLabel("Services musicaux")
            ServicesRow(activeCp = state.nowPlaying.cpName)

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialHost = vm.host,
            initialPort = vm.port,
            onDismiss = { showSettings = false },
            onSave = { h, p -> vm.updateEndpoint(h, p); showSettings = false },
        )
    }

    if (showUrlDialog) {
        UrlDialog(
            onDismiss = { showUrlDialog = false },
            onPlay = { url -> vm.playUrl(url); showUrlDialog = false },
        )
    }
}

@Composable
private fun Header(name: String, reachable: Boolean, onRefresh: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (reachable) Color(0xFF7CB342) else Color(0xFFE53935)),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = name,
            color = Amber,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir", tint = Amber)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Réglages", tint = Amber)
        }
    }
}

@Composable
private fun UnreachableBanner(error: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0E0E)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Barre injoignable", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
            Text(
                error ?: "Vérifiez l'IP/le port et le Wi-Fi de la barre (bug de perte de config connu).",
                color = Color(0xFFE57373),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun NowPlayingCard(state: com.k650.remote.data.SoundbarState) {
    val np = state.nowPlaying
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art with an amber halo.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Amber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B0906)),
                contentAlignment = Alignment.Center,
            ) {
                if (!np.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = np.thumbnailUrl,
                        contentDescription = "Pochette",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Text("♪", color = Amber, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = np.title?.takeIf { it.isNotBlank() } ?: "Aucune lecture",
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = np.artist ?: "",
                    color = Amber.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!np.cpName.isNullOrBlank()) {
                    Text(
                        text = np.cpName,
                        color = Amber.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutsRow(onBluetooth: () -> Unit, onPlayUrl: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onBluetooth,
            colors = ButtonDefaults.buttonColors(containerColor = AmberDim, contentColor = Amber),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Bluetooth (YouTube)")
        }
        Button(
            onClick = onPlayUrl,
            colors = ButtonDefaults.buttonColors(containerColor = AmberDim, contentColor = Amber),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.Link, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Lire une URL")
        }
    }
}

@Composable
private fun ServicesRow(activeCp: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MusicService.visible.forEach { svc ->
            val active = activeCp?.equals(svc.label, ignoreCase = true) == true
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Amber else Color(0xFF14100A))
                    .border(1.dp, AmberDim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    svc.label,
                    color = if (active) Color.Black else Amber,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Amber.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

// ---- Dialogs ----------------------------------------------------------------

@Composable
private fun SettingsDialog(
    initialHost: String,
    initialPort: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(host.trim(), port.trim().toIntOrNull() ?: initialPort) }) {
                Text("Enregistrer", color = Amber)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Amber) } },
        title = { Text("Réglages de la barre", color = Amber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Adresse IP") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit) },
                    label = { Text("Port") },
                    singleLine = true,
                )
                Text(
                    "Réservez une IP DHCP statique pour la barre.",
                    color = Amber.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
            }
        },
        containerColor = PanelSurface,
    )
}

@Composable
private fun UrlDialog(onDismiss: () -> Unit, onPlay: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { if (url.isNotBlank()) onPlay(url) }) {
                Text("Lire", color = Amber)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Amber) } },
        title = { Text("Lire une URL audio", color = Amber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("http://… (NAS, webradio, TTS)") },
                    singleLine = true,
                )
                Text(
                    "Flux audio HTTP direct. N'injectez pas de flux YouTube extraits (CGU).",
                    color = Amber.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
            }
        },
        containerColor = PanelSurface,
    )
}
