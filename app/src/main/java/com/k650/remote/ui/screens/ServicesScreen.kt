package com.k650.remote.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k650.remote.data.CpService
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.components.GlassCard
import com.k650.remote.ui.components.SectionHeader
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.Good
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.OnInkMed
import com.k650.remote.ui.theme.Teal

/** Services we surface (best-known + firmware-supported). Others are hidden. */
private fun isCurated(name: String): Boolean = name.lowercase().let {
    it.contains("spotify") || it.contains("tunein") || it.contains("deezer") ||
        it.contains("qobuz") || it.contains("tidal")
}

@Composable
fun ServicesScreen(vm: SoundbarViewModel) {
    val state by vm.services.collectAsState()
    var loginFor by remember { mutableStateOf<CpService?>(null) }

    LaunchedEffect(Unit) { vm.loadServices() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.activeServiceId != null && state.breadcrumb.isNotEmpty()) {
            BrowseView(vm)
            return@Column
        }

        SectionHeader("Services musicaux") {
            if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = Amber)
        }

        val curated = state.services.filter { isCurated(it.name) }
        if (curated.isEmpty() && !state.loading) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aucun service lu depuis la barre. Assurez-vous qu'elle est joignable (onglet Réglages), " +
                        "puis rafraîchissez. La liste des services est gravée dans le firmware et lue via GetCpList.",
                    color = OnInkMed, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        curated.forEach { svc -> ServiceRow(svc, vm) { loginFor = svc } }

        SectionHeader("YouTube · Amazon Music")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "Non pris en charge par le firmware de la K650. Diffusez-les en Bluetooth depuis le téléphone.",
                    color = OnInkMed, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(10.dp))
                OutlinedButton(onClick = vm::switchToBluetooth) {
                    Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp)); Text("Basculer en Bluetooth")
                }
            }
        }
    }

    loginFor?.let { svc ->
        LoginDialog(service = svc, onDismiss = { loginFor = null }, onLogin = { u, p -> vm.login(svc, u, p); loginFor = null })
    }
}

@Composable
private fun ServiceRow(svc: CpService, vm: SoundbarViewModel, onLogin: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(svc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val sub = when {
                        svc.isSpotify -> "Spotify Connect — piloté depuis l'app Spotify"
                        svc.isTuneIn -> "Radios & podcasts — sans compte"
                        svc.signedIn -> "Connecté${svc.username?.let { " · $it" } ?: ""}"
                        svc.supportsLogin -> "Non connecté"
                        else -> ""
                    }
                    if (sub.isNotBlank()) {
                        Text(sub, color = if (svc.signedIn) Good else OnInkFaint, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (svc.signedIn) {
                    IconButton(onClick = { vm.logout(svc) }) { Icon(Icons.Filled.Logout, contentDescription = "Déconnexion", tint = OnInkMed) }
                } else if (svc.supportsLogin) {
                    IconButton(onClick = onLogin) { Icon(Icons.Filled.Login, contentDescription = "Connexion", tint = Teal) }
                }
            }
            if (!svc.isSpotify) {
                Spacer(Modifier.size(8.dp))
                OutlinedButton(onClick = { vm.openService(svc) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Parcourir")
                }
            }
        }
    }
}

@Composable
private fun BrowseView(vm: SoundbarViewModel) {
    val state by vm.services.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { vm.loadServices() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour aux services", tint = Amber)
        }
        Text(
            state.breadcrumb.joinToString(" › "),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (state.browsing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = Amber)
    }
    Spacer(Modifier.size(8.dp))

    if (state.items.isEmpty() && !state.browsing) {
        Text("Rien à afficher ici.", color = OnInkFaint, style = MaterialTheme.typography.bodyMedium)
    }
    state.items.forEach { item ->
        GlassCard(modifier = Modifier.fillMaxWidth().clickable { vm.openItem(item) }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(item.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(
                    imageVector = if (item.playable) Icons.Filled.PlayArrow else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = if (item.playable) Amber else OnInkFaint,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}
