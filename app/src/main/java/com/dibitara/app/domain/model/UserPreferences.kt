package com.dibitara.app.domain.model

/**
 * Préférences de l'utilisateur stockées localement via DataStore.
 * [seuilFondsCents]          : seuil en-dessous duquel une alerte "liquidités insuffisantes" est envoyée.
 * [deviseParDefaut]          : devise utilisée à l'affichage et à la saisie.
 * [dashboardCardOrder]       : ordre des cartes reordonnables du tableau de bord.
 * [notificationsMensuelles]  : si true, un résumé mensuel est envoyé en notification le 1er du mois.
 * [afficherRecommandations]  : si true, le bouton "Recommandations" apparaît dans l'écran Budget.
 * [tauxEpargneCiblePct]      : objectif d'épargne en % du revenu mensuel (défaut : 20 % - règle 50/30/20).
 */
data class UserPreferences(
    val seuilFondsCents: Long = 20_000L,
    val deviseParDefaut: Currency = Currency.EUR,
    val afficherRapportMensuel: Boolean = false,
    val afficherEpargne: Boolean = true,
    val afficherInvestissements: Boolean = true,
    val twoFactorEnabled: Boolean = false,
    val afficherProchainsPaiements: Boolean = true,
    val dashboardCardOrder: List<DashboardCard> = DashboardCard.entries.toList(),
    val notificationsMensuelles: Boolean = false,
    val afficherRecommandations: Boolean = false,
    val tauxEpargneCiblePct: Int = 20
)
