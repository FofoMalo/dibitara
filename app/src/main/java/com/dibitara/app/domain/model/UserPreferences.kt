package com.dibitara.app.domain.model

/**
 * Préférences de l'utilisateur stockées localement via DataStore.
 * [seuilFondsCents]          : seuil en-dessous duquel une alerte "liquidités insuffisantes" est envoyée.
 * [deviseParDefaut]          : devise utilisée à l'affichage et à la saisie.
 * [dashboardCardOrder]       : ordre des cartes reordonnables du tableau de bord.
 * [notificationsMensuelles]  : si true, un résumé mensuel est envoyé en notification le 1er du mois.
 * [afficherRecommandations]  : si true, le bouton "Recommandations" apparaît dans l'écran Budget.
 * [tauxEpargneCiblePct]      : objectif d'épargne en % du revenu mensuel (défaut : 20 % - règle 50/30/20).
 * [derniereImportEpochMilli] : date + heure du dernier import CSV/PDF réussi (BRED ou TradeRepublic), null si aucun import.
 * [masquerMontants]          : si true, tous les montants affichés à l'écran sont remplacés par "••••" (confidentialité).
 * [themeMode]                : apparence choisie (système/clair/sombre) - voir [ThemeMode].
 * [derniereAlerteFondsEpochDay] : jour (epoch day) de la dernière notification "liquidités
 *   insuffisantes" envoyée - évite de renotifier à chaque ouverture de l'app le même jour
 *   tant que le solde reste sous le seuil.
 * [derniereAlerteBudgetEpochDay] : jour (epoch day) de la dernière notification "budget
 *   dépassé" envoyée - même logique que [derniereAlerteFondsEpochDay].
 * [derniereAlerteDettesEpochDay] : jour (epoch day) du dernier envoi des rappels d'échéance
 *   dette - même logique, gate le lot entier plutôt qu'une dette à la fois (une dette n'est
 *   de toute façon retournée par [com.dibitara.app.domain.usecase.CheckDebtRemindersUseCase]
 *   que le jour de son échéance).
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
    val tauxEpargneCiblePct: Int = 20,
    val derniereImportEpochMilli: Long? = null,
    val masquerMontants: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEME,
    val derniereAlerteFondsEpochDay: Long? = null,
    val derniereAlerteBudgetEpochDay: Long? = null,
    val derniereAlerteDettesEpochDay: Long? = null
)
