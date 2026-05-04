// Fichier de build racine : déclare les plugins disponibles pour tous les modules.
// Aucune dépendance applicative ici — tout va dans app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.ksp)                  apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.kover)                apply false
}
