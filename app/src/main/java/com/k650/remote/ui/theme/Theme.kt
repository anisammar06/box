package com.k650.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The look is a 90s amber matrix panel: warm amber on near-black.
val Amber = Color(0xFFFFB000)
val AmberBright = Color(0xFFFFC94D)
val AmberDim = Color(0xFF3A2A00)
val PanelBlack = Color(0xFF070604)
val PanelSurface = Color(0xFF14100A)

private val K650Colors = darkColorScheme(
    primary = Amber,
    onPrimary = PanelBlack,
    secondary = AmberBright,
    background = PanelBlack,
    onBackground = Amber,
    surface = PanelSurface,
    onSurface = Amber,
    surfaceVariant = AmberDim,
)

@Composable
fun K650Theme(content: @Composable () -> Unit) {
    // Deliberately always the amber panel look, regardless of system dark mode.
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = K650Colors, content = content)
}
