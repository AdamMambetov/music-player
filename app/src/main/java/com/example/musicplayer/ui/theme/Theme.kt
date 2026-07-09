package com.example.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkAmberScheme = darkColorScheme(
    primary = Amber60,
    onPrimary = SurfaceDark,
    secondary = Amber40,
    onSecondary = SurfaceDark,
    background = SurfaceDark,
    onBackground = OnSurfacePrimary,
    surface = SurfaceVariant,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = OnSurfaceSecondary,
    outline = DividerColor,
    error = AccentRed,
)

@Composable
fun MusicPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkAmberScheme,
        typography = Typography,
        content = content
    )
}
