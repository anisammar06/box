package com.k650.remote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val K650Colors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    primaryContainer = AmberDeep,
    onPrimaryContainer = OnInkHigh,
    secondary = Teal,
    onSecondary = Ink,
    tertiary = Violet,
    onTertiary = Ink,
    background = Ink,
    onBackground = OnInkHigh,
    surface = Surface1,
    onSurface = OnInkHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = OnInkMed,
    outline = Outline,
    outlineVariant = Surface3,
    error = Bad,
    onError = Ink,
)

private val K650Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val K650Typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, color = OnInkMed),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    ),
)

@Composable
fun K650Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = K650Colors,
        shapes = K650Shapes,
        typography = K650Typography,
        content = content,
    )
}
