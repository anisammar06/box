package com.k650.remote.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.k650.remote.ui.screens.NowPlayingScreen
import com.k650.remote.ui.screens.ServicesScreen
import com.k650.remote.ui.screens.SettingsScreen
import com.k650.remote.ui.screens.SoundScreen
import com.k650.remote.ui.screens.SourcesScreen
import com.k650.remote.ui.theme.Ink
import com.k650.remote.ui.theme.Surface1

private enum class Tab(val label: String, val icon: ImageVector) {
    NOW_PLAYING("Lecture", Icons.Filled.PlayCircleFilled),
    SOURCES("Sources", Icons.Filled.Speaker),
    SOUND("Son", Icons.Filled.Tune),
    SERVICES("Services", Icons.Filled.LibraryMusic),
    SETTINGS("Réglages", Icons.Outlined.Settings),
}

@Composable
fun MainScaffold(vm: SoundbarViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.NOW_PLAYING) }
    val snackText by vm.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackText) {
        snackText?.let { snackbarHostState.showSnackbar(it); vm.consumeSnackbar() }
    }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.NOW_PLAYING -> NowPlayingScreen(vm)
                Tab.SOURCES -> SourcesScreen(vm)
                Tab.SOUND -> SoundScreen(vm)
                Tab.SERVICES -> ServicesScreen(vm)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
