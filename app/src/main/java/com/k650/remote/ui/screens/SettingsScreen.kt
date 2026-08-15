package com.k650.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.k650.remote.data.DeviceInfo
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.components.GlassCard
import com.k650.remote.ui.components.SectionHeader
import com.k650.remote.ui.components.StatusDot
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.Bad
import com.k650.remote.ui.theme.Good
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.OnInkMed

@Composable
fun SettingsScreen(vm: SoundbarViewModel) {
    val state by vm.state.collectAsState()
    var host by remember { mutableStateOf(vm.host) }
    var port by remember { mutableStateOf(vm.port.toString()) }
    var showRename by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- Connection state ----
        SectionHeader("État de la connexion")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(
                    color = if (state.reachable) Good else Bad,
                    label = if (state.reachable) "Barre joignable" else "Barre injoignable",
                )
                InfoLine("Modèle", state.device.modelName)
                InfoLine("Adresse MAC", state.device.macAddress)
                InfoLine("Firmware", state.device.softwareVersion)
                if (!state.reachable && state.lastError != null) {
                    Text(state.lastError!!, color = Bad, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ---- Wi-Fi ----
        SectionHeader("Réseau Wi-Fi de la barre")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoLine("SSID", state.device.wifiSsid)
                InfoLine("Signal", state.device.wifiRssi?.let { "$it dBm${state.device.wifiBars?.let { b -> " · $b/4" } ?: ""}" })
                InfoLine("Canal", state.device.wifiChannel)
                InfoLine("Type", state.device.connectionType)
                Text(
                    "Bug connu : la K650 perd sa config Wi-Fi tous les 2–3 jours. Activez « Veille du " +
                        "réseau Soundbar », un bail DHCP statique et long, et désactivez le PMF (802.11w) sur le SSID 2,4 GHz.",
                    color = OnInkFaint, style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // ---- Endpoint ----
        SectionHeader("Adresse de la barre")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Adresse IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit) }, label = { Text("Port (55001)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { vm.updateEndpoint(host.trim(), port.toIntOrNull() ?: 55001) },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = androidx.compose.ui.graphics.Color.Black),
                ) { Text("Appliquer") }
                Text("Réservez une IP DHCP statique pour la barre.", color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
            }
        }

        // ---- Power / standby ----
        SectionHeader("Alimentation & veille")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { vm.standby(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = androidx.compose.ui.graphics.Color.Black),
                        modifier = Modifier.weight(1f),
                    ) { Text("Mettre en veille") }
                    OutlinedButton(onClick = vm::cancelStandby, modifier = Modifier.weight(1f)) { Text("Annuler minuterie") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(15, 30, 60).forEach { min ->
                        OutlinedButton(onClick = { vm.standby(min * 60) }, modifier = Modifier.weight(1f)) { Text("$min min") }
                    }
                }
                Text(
                    "L'API de la barre ne permet PAS de la rallumer à distance (pas de commande d'allumage " +
                        "réseau sur ce firmware) : utilisez la télécommande, le HDMI-CEC du téléviseur ou le réveil " +
                        "Bluetooth. Seule la mise en veille est pilotable ici.",
                    color = OnInkFaint, style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // ---- Device ----
        SectionHeader("Barre")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("Nom", state.speakerName)
                OutlinedButton(onClick = { showRename = true }) { Text("Renommer") }
            }
        }

        // ---- About ----
        SectionHeader("À propos")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Pilotage local de la Samsung HW-K650 via son API HTTP non authentifiée (WAM/UIC/CPM, " +
                    "port 55001). Le VU-mètre est décoratif — l'API ne fournit aucun niveau réel. Le bouton " +
                    "« précédent » redémarre le morceau courant (le firmware n'a pas de vraie commande précédent).",
                color = OnInkMed, style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.size(8.dp))
    }

    if (showRename) {
        RenameDialog(initial = state.speakerName ?: "", onDismiss = { showRename = false }, onSave = { vm.rename(it); showRename = false })
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = OnInkFaint, style = MaterialTheme.typography.bodyMedium)
        Text(value ?: "—", color = if (value != null) MaterialTheme.colorScheme.onSurface else OnInkFaint, fontWeight = FontWeight.Medium)
    }
}
