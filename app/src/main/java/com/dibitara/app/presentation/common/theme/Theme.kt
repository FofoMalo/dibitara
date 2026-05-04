package com.dibitara.app.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DibitaraGreen  = Color(0xFF1DB954)
private val DibitaraDark   = Color(0xFF121212)
private val DibitaraSurface = Color(0xFF1E1E1E)

private val DarkColors = darkColorScheme(
    primary   = DibitaraGreen,
    background = DibitaraDark,
    surface   = DibitaraSurface,
)

private val LightColors = lightColorScheme(
    primary = DibitaraGreen,
)

@Composable
fun DibitaraTheme(
    darkTheme: Boolean = true, // Finance apps préfèrent le dark mode par défaut
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
