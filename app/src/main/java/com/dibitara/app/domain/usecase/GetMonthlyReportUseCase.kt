package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryExpense
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Construit le rapport mensuel en combinant 4 sources de données :
 *  - Transactions du mois courant (revenus, dépenses, catégories)
 *  - Transactions du mois précédent (pour calculer la variation)
 *  - Budget du mois courant
 *  - Sous-catégories personnalisées (pour résoudre les noms par id)
 *
 * Retourne un Flow actif : le rapport se recalcule automatiquement
 * si l'une des sources est modifiée.
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val getMonthlyBudget: GetMonthlyBudgetUseCase,
    private val getCustomSubCategories: GetCustomSubCategoriesUseCase
) {
    operator fun invoke(month: Int, year: Int): Flow<MonthlyReport> {
        val datePrecedente = LocalDate.of(year, month, 1).minusMonths(1)

        return combine(
            getMonthlyTransactions(month, year),
            getMonthlyTransactions(datePrecedente.monthValue, datePrecedente.year),
            getMonthlyBudget(month, year),
            getCustomSubCategories()
        ) { current, previous, budget, customSubCats ->

            val revenus  = current.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountCents }
            val depenses = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            val depensesPrecedent = previous.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }

            // Pour AUTRE : on éclate par sous-catégorie pour éviter un bloc "Autre" dominant.
            // Clé de regroupement : catégorie normale, ou sous-catégorie si category == AUTRE.
            val topCategories = current
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { tx ->
                    when {
                        tx.category == Category.AUTRE && tx.subCategory != null ->
                            "subcat_${tx.subCategory.name}"
                        tx.category == Category.AUTRE && tx.customSubCategoryId != null ->
                            "custom_${tx.customSubCategoryId}"
                        else -> "cat_${tx.category.name}"
                    }
                }
                .map { (key, transactions) ->
                    val total      = transactions.sumOf { it.amountCents }
                    val firstTx    = transactions.first()
                    val label = when {
                        key.startsWith("subcat_") ->
                            firstTx.subCategory!!.displayName
                        key.startsWith("custom_") ->
                            customSubCats.find { it.id == firstTx.customSubCategoryId }?.name
                                ?: Category.AUTRE.displayName
                        else -> firstTx.category.displayName
                    }
                    CategoryExpense(
                        category     = firstTx.category,
                        totalCents   = total,
                        pourcentage  = if (depenses > 0) total.toFloat() / depenses * 100f else 0f,
                        displayLabel = label
                    )
                }
                .sortedByDescending { it.totalCents }
                .take(5)

            val devise = current.firstOrNull()?.currency ?: Currency.EUR

            MonthlyReport(
                month                   = month,
                year                    = year,
                currency                = devise,
                revenusCents            = revenus,
                depensesCents           = depenses,
                soldeCents              = revenus - depenses,
                budget                  = budget,
                topCategories           = topCategories,
                variationDepensesCents  = depenses - depensesPrecedent
            )
        }
    }
}
