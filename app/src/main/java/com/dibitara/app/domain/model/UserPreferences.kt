package com.dibitara.app.domain.model

/**
 * Préférences de l'utilisateur stockées localement via DataStore.
 * [seuilFondsCents] : seuil en-dessous duquel une alerte "liquidités insuffisantes" est envoyée.
 * [deviseParDefaut] : devise utilisée à l'affichage et à la saisie.
 */
data class UserPreferences(
    val seuilFondsCents: Long = 50_000L,        // 500€ par défaut
    val deviseParDefaut: Currency = Currency.EUR,
    val afficherRapportMensuel: Boolean = false  // remplace le graphique 6 mois dans le Dashboard
)
