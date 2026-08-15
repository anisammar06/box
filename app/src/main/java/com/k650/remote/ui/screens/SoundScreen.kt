package com.k650.remote.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k650.remote.data.SoundbarState
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.components.GlassCard
import com.k650.remote.ui.components.SectionHeader
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.OnInkMed
import com.k650.remote.ui.theme.Outline

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundScreen(vm: SoundbarViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Égaliseur")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (state.eqPresets.isEmpty()) {
                Text(
                    "Aucun preset rapporté par la barre pour l'instant. Les presets 7 bandes " +
                        "sont lus via Get7bandEQList au prochain sondage — vérifiez que la barre est joignable.",
                    color = OnInkMed, style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.eqPresets.forEach { preset ->
                        val selected = state.currentEqIndex == preset.index
                        OutlinedButton(
                            onClick = { vm.selectEqPreset(preset.index) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) Amber else Color.Transparent,
                                contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            ),
                        ) { Text(preset.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
            }
        }

        SectionHeader("Niveau du caisson")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("−6", color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = state.wooferLevel.toFloat(),
                        onValueChange = { vm.setWoofer(it.toInt()) },
                        valueRange = SoundbarState.WOOFER_MIN.toFloat()..SoundbarState.WOOFER_MAX.toFloat(),
                        steps = (SoundbarState.WOOFER_MAX - SoundbarState.WOOFER_MIN) - 1,
                        colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber, inactiveTrackColor = Outline),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text("+6", color = OnInkFaint, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    "Caisson : ${if (state.wooferLevel > 0) "+" else ""}${state.wooferLevel}",
                    color = Amber, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        SectionHeader("Non disponible via l'API locale")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Mode nuit, DRC, amplification des dialogues et synchro audio ne sont exposés que " +
                    "par le cloud SmartThings de Samsung, pas par l'API locale de la barre (port 55001). " +
                    "Ils ne peuvent donc pas être pilotés hors ligne depuis cette app.",
                color = OnInkMed, style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
