package com.k650.remote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k650.remote.data.MusicService
import com.k650.remote.data.Source
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.components.GlassCard
import com.k650.remote.ui.components.SectionHeader
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.OnInkMed
import com.k650.remote.ui.theme.Teal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcesScreen(vm: SoundbarViewModel) {
    val state by vm.state.collectAsState()
    var showUrl by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Source d'entrée")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Source.entries.forEach { s ->
                    val selected = state.source == s
                    OutlinedButton(
                        onClick = { vm.selectSource(s) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Amber else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (selected) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text(s.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }

        SectionHeader("Raccourcis")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = vm::switchToBluetooth,
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = androidx.compose.ui.graphics.Color.Black),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null); Spacer(Modifier.size(6.dp)); Text("Bluetooth")
            }
            OutlinedButton(onClick = { showUrl = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Link, contentDescription = null); Spacer(Modifier.size(6.dp)); Text("Lire une URL")
            }
        }

        SectionHeader("Streaming non natif")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "La K650 (2016) ne connaît pas YouTube, YouTube Music ni Amazon Music : " +
                        "ce ne sont pas des services de son firmware et elle n'a pas de Chromecast. " +
                        "La voie propre est le Bluetooth depuis le téléphone.",
                    color = OnInkMed,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(10.dp))
                FlowRowNonNative(vm)
            }
        }
    }

    if (showUrl) {
        UrlDialog(onDismiss = { showUrl = false }, onPlay = { vm.playUrl(it); showUrl = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowNonNative(vm: SoundbarViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MusicService.bluetoothOnly.forEach { svc ->
            OutlinedButton(onClick = vm::switchToBluetooth) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(svc.label)
            }
        }
    }
}
