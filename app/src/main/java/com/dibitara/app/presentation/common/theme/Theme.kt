package com.dibitara.app.presentation.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Schéma dark - mode par défaut pour Dibitara ─────────────────────────────
//
// Finance apps + fond sombre = meilleur confort de lecture la nuit
// et mise en valeur de l'or (#E6C675) sur noir (#111416).

private val DibitaraDarkColorScheme = darkColorScheme(
    primary              = md_dark_primary,
    onPrimary            = md_dark_onPrimary,
    primaryContainer     = md_dark_primaryContainer,
    onPrimaryContainer   = md_dark_onPrimaryContainer,
    secondary            = md_dark_secondary,
    onSecondary          = md_dark_onSecondary,
    secondaryContainer   = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary             = md_dark_tertiary,
    onTertiary           = md_dark_onTertiary,
    tertiaryContainer    = md_dark_tertiaryContainer,
    onTertiaryContainer  = md_dark_onTertiaryContainer,
    error                = md_dark_error,
    onError              = md_dark_onError,
    errorContainer       = md_dark_errorContainer,
    onErrorContainer     = md_dark_onErrorContainer,
    background           = md_dark_background,
    onBackground         = md_dark_onBackground,
    surface              = md_dark_surface,
    onSurface            = md_dark_onSurface,
    surfaceVariant       = md_dark_surfaceVariant,
    onSurfaceVariant     = md_dark_onSurfaceVariant,
    outline              = md_dark_outline,
    outlineVariant       = md_dark_outlineVariant,
    surfaceContainerLowest = md_dark_background,
    surfaceContainerLow = md_dark_surface,
    surfaceContainer = md_dark_surface,
    surfaceContainerHigh = md_dark_surfaceVariant,
    surfaceContainerHighest = md_dark_surfaceVariant,
)

// ─── Schéma light ─────────────────────────────────────────────────────────────

private val DibitaraLightColorScheme = lightColorScheme(
    primary              = md_light_primary,
    onPrimary            = md_light_onPrimary,
    primaryContainer     = md_light_primaryContainer,
    onPrimaryContainer   = md_light_onPrimaryContainer,
    secondary            = md_light_secondary,
    onSecondary          = md_light_onSecondary,
    secondaryContainer   = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary             = md_light_tertiary,
    onTertiary           = md_light_onTertiary,
    tertiaryContainer    = md_light_tertiaryContainer,
    onTertiaryContainer  = md_light_onTertiaryContainer,
    error                = md_light_error,
    onError              = md_light_onError,
    errorContainer       = md_light_errorContainer,
    onErrorContainer     = md_light_onErrorContainer,
    background           = md_light_background,
    onBackground         = md_light_onBackground,
    surface              = md_light_surface,
    onSurface            = md_light_onSurface,
    surfaceVariant       = md_light_surfaceVariant,
    onSurfaceVariant     = md_light_onSurfaceVariant,
    outline              = md_light_outline,
    outlineVariant       = md_light_outlineVariant,
    surfaceContainerLowest = md_light_background,
    surfaceContainerLow = md_light_surface,
    surfaceContainer = md_light_surface,
    surfaceContainerHigh = md_light_surfaceVariant,
    surfaceContainerHighest = md_light_surfaceVariant,
)

// ─── Thème racine ─────────────────────────────────────────────────────────────

@Composable
fun DibitaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val base = Typography()
    MaterialTheme(
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        typography = base.copy(
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp)
        ),
        colorScheme = if (darkTheme) DibitaraDarkColorScheme else DibitaraLightColorScheme,
        content = content
    )
}
