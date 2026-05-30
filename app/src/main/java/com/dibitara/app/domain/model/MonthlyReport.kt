package com.dibitara.app.domain.model

/**
 * Synthèse financière d'un mois donné, calculée par [GetMonthlyReportUseCase].
 *
 * [topCategories] contient au maximum 3 catégories triées par montant décroissant.
 * [variationDepensesCents] > 0 signifie plus de dépenses que le mois précédent.
 * [tauxEpargnePct] = (revenus - dépenses) * 100 / revenus, null si revenus == 0.
 * [variationParCategorie] = top 5 catégories par variation absolue M/M-1.
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
    val variationDepensesCents: Long,
    val tauxEpargnePct: Int? = null,
    val variationParCategorie: List<CategoryVariation> = emptyList()
)

/**
 * Part d'une catégorie (ou sous-catégorie) dans les dépenses du mois.
 * [pourcentage] est entre 0.0 et 100.0.
 * [displayLabel] remplace [category.displayName] pour les sous-catégories d'AUTRE,
 * afin d'afficher "Cadeaux" ou "Netflix" au lieu de "Autre".
 */
data class CategoryExpense(
    val category: Category,
    val totalCents: Long,
    val pourcentage: Float,
    val displayLabel: String = category.displayName
)

/**
 * Variation M/M-1 pour une catégorie de dépenses.
 * [variationCents] > 0 = plus de dépenses que le mois précédent (mauvais signe).
 */
data class CategoryVariation(
    val category: Category,
    val displayLabel: String,
    val currentCents: Long,
    val variationCents: Long
)
