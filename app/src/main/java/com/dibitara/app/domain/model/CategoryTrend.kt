package com.dibitara.app.domain.model

/**
 * Tendance de dépenses pour une catégorie sur les 6 derniers mois.
 *
 * [moisData] contient 6 entrées ordonnées du plus ancien au plus récent.
 * [variationPct] = ((moisN - moisN-1) * 100 / moisN-1), null si moisN-1 == 0.
 * [totalSixMoisCents] = somme des 6 mois pour le tri global.
 */
data class CategoryTrend(
    val category: Category,
    val moisData: List<MonthlyAmount>,
    val totalSixMoisCents: Long,
    val variationPct: Int?
)

/** Montant total pour un mois et une année donnés. */
data class MonthlyAmount(val month: Int, val year: Int, val totalCents: Long)
