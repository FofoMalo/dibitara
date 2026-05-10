package com.dibitara.app.presentation.common.theme

import androidx.compose.ui.graphics.Color

// ─── Palette de marque Dibitara ───────────────────────────────────────────────
//
// Philosophie : "fin de l'obscurantisme financier"
//   → Noir profond (#0D0D0D) = clarté, sobriété, confiance
//   → Or (#F5C542)           = valeur, ambition, lumière
//   → Blanc cassé (#FAFAFA)  = lisibilité, neutralité
//
// Les rôles Material 3 (primary, surface, error…) sont dérivés de cette base.

// Couleurs source
val DibitaraOr       = Color(0xFFF5C542)   // or principal — action, CTA
val DibitaraNoir     = Color(0xFF0D0D0D)   // fond sombre
val DibitaraNeutre   = Color(0xFFFAFAFA)   // texte et surfaces claires

// ─── Rôles dark (utilisés par défaut) ────────────────────────────────────────

// Primary — or Dibitara
val md_dark_primary              = Color(0xFFF5C542)   // boutons, FAB, liens actifs
val md_dark_onPrimary            = Color(0xFF3D2E00)   // texte sur bouton doré
val md_dark_primaryContainer     = Color(0xFF574400)   // chip, surface d'accent
val md_dark_onPrimaryContainer   = Color(0xFFFFE08A)   // texte sur container doré

// Secondary — ton chaud neutre (complémentaire de l'or)
val md_dark_secondary            = Color(0xFFD4B896)
val md_dark_onSecondary          = Color(0xFF3A2E1E)
val md_dark_secondaryContainer   = Color(0xFF523E2A)
val md_dark_onSecondaryContainer = Color(0xFFF2D9BC)

// Tertiary — vert sage (montants positifs, revenus)
val md_dark_tertiary             = Color(0xFFA8C7A0)
val md_dark_onTertiary           = Color(0xFF12361C)
val md_dark_tertiaryContainer    = Color(0xFF294D30)
val md_dark_onTertiaryContainer  = Color(0xFFC4E3BC)

// Error — rouge standard M3
val md_dark_error                = Color(0xFFCF6679)
val md_dark_onError              = Color(0xFF690018)
val md_dark_errorContainer       = Color(0xFF93000A)
val md_dark_onErrorContainer     = Color(0xFFFFDAD6)

// Background & Surface
val md_dark_background           = Color(0xFF0D0D0D)   // fond global
val md_dark_onBackground         = Color(0xFFFAFAFA)
val md_dark_surface              = Color(0xFF1A1A1A)   // cartes, bottom sheets
val md_dark_onSurface            = Color(0xFFFAFAFA)
val md_dark_surfaceVariant       = Color(0xFF2C2B26)   // variante tiède
val md_dark_onSurfaceVariant     = Color(0xFFCEC6AA)   // texte secondaire

// Outline
val md_dark_outline              = Color(0xFF979080)
val md_dark_outlineVariant       = Color(0xFF4B4839)

// ─── Rôles light ─────────────────────────────────────────────────────────────

val md_light_primary             = Color(0xFF6B5300)   // or foncé lisible sur fond clair
val md_light_onPrimary           = Color(0xFFFFFFFF)
val md_light_primaryContainer    = Color(0xFFFFE08A)
val md_light_onPrimaryContainer  = Color(0xFF221A00)

val md_light_secondary           = Color(0xFF6A5D3F)
val md_light_onSecondary         = Color(0xFFFFFFFF)
val md_light_secondaryContainer  = Color(0xFFF4E0BB)
val md_light_onSecondaryContainer= Color(0xFF231A05)

val md_light_tertiary            = Color(0xFF3D6641)
val md_light_onTertiary          = Color(0xFFFFFFFF)
val md_light_tertiaryContainer   = Color(0xFFC4E3BC)
val md_light_onTertiaryContainer = Color(0xFF002109)

val md_light_error               = Color(0xFFBA1A1A)
val md_light_onError             = Color(0xFFFFFFFF)
val md_light_errorContainer      = Color(0xFFFFDAD6)
val md_light_onErrorContainer    = Color(0xFF410002)

val md_light_background          = Color(0xFFFAFAFA)
val md_light_onBackground        = Color(0xFF1C1B16)
val md_light_surface             = Color(0xFFFAFAFA)
val md_light_onSurface           = Color(0xFF1C1B16)
val md_light_surfaceVariant      = Color(0xFFEDE1CF)
val md_light_onSurfaceVariant    = Color(0xFF4D4639)

val md_light_outline             = Color(0xFF7F7560)
val md_light_outlineVariant      = Color(0xFFD0C5B0)
