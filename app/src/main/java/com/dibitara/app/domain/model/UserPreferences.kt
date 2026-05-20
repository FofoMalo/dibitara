package com.dibitara.app.domain.model

/**
 * Préférences de l'utilisateur stockées localement via DataStore.
 * [seuilFondsCents] : seuil en-dessous duquel une alerte "liquidités insuffisantes" est envoyée.
 * [deviseParDefaut] : devise utilisée à l'affichage et à la saisie.
 */
data class UserPreferences(
    val seuilFondsCents: Long = 20_000L,
    val deviseParDefaut: Currency = Currency.EUR,
    val afficherRapportMensuel: Boolean = false,
    val afficherEpargne: Boolean = true,          // onglet "Épargne" visible dans la nav bar
    val afficherInvestissements: Boolean = true,  // onglet "Placements" visible dans la nav bar
    val twoFactorEnabled: Boolean = false,        // TOTP requis après PIN ou mot de passe
    val afficherProchainsPaiements: Boolean = true   // carte "Prochains paiements" sur le Dashboard
)
