package com.dibitara.app.domain.model

/**
 * Préférence d'apparence de l'application.
 * SYSTEME suit le thème clair/sombre du système Android (comportement par défaut,
 * identique à l'ancien binding [androidx.compose.foundation.isSystemInDarkTheme]).
 */
enum class ThemeMode(val displayName: String) {
    SYSTEME("Système"),
    CLAIR("Clair"),
    SOMBRE("Sombre")
}
