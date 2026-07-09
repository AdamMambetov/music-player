package com.example.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkBlueScheme = darkColorScheme(
    primary = Blue60,
    onPrimary = SurfaceDark,
    secondary = Blue40,
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
        colorScheme = DarkBlueScheme,
        typography = Typography,
        content = content
    )
}
