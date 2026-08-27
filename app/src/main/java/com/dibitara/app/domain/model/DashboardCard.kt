package com.dibitara.app.domain.model

/**
 * Identifie chaque carte reordonnable du tableau de bord.
 * La carte PATRIMOINE est fixée en tête et n'apparaît pas ici.
 *
 * L'ordre par défaut ([entries]) correspond à l'affichage initial de l'application.
 * L'utilisateur peut le personnaliser depuis le mode édition du dashboard.
 */
enum class DashboardCard {
    METRIQUES_BUDGET_EPARGNE,
    METRIQUES_INVESTISSEMENTS,
    DETTES,
    CASHFLOW_PROJECTION,
    RAPPORT_GRAPHIQUE,
    SUGGESTIONS_RECATEGORISATION,
    ENVELOPPES_ALERTE,
    PROCHAINS_PAIEMENTS,
    COMPTES
}
