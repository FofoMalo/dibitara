package com.dibitara.app.domain.model

/**
 * Synthèse financière d'un mois donné, calculée par [GetMonthlyReportUseCase].
 *
 * [topCategories] contient au maximum 3 catégories triées par montant décroissant.
 * [variationDepensesCents] > 0 signifie plus de dépenses que le mois précédent.
 */
data class MonthlyReport(
    val month: Int,
    val year: Int,
    val currency: Currency,
    val revenusCents: Long,
    val depensesCents: Long,
    val soldeCents: Long,
    val budget: Budget?,
    val topCategories: List<CategoryExpense>,
    val variationDepensesCents: Long
)

/**
 * Part d'une catégorie dans les dépenses du mois.
 * [pourcentage] est entre 0.0 et 100.0.
 */
data class CategoryExpense(
    val category: Category,
    val totalCents: Long,
    val pourcentage: Float
)
