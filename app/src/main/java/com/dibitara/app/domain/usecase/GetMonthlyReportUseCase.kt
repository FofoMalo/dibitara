package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryExpense
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Construit le rapport mensuel en combinant 3 sources de données :
 *  - Transactions du mois courant (revenus, dépenses, catégories)
 *  - Transactions du mois précédent (pour calculer la variation)
 *  - Budget du mois courant
 *
 * Retourne un Flow actif : le rapport se recalcule automatiquement
 * si l'une des sources est modifiée.
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val getMonthlyBudget: GetMonthlyBudgetUseCase
) {
    operator fun invoke(month: Int, year: Int): Flow<MonthlyReport> {
        // Calcul du mois précédent en tenant compte du changement d'année (ex. jan → déc)
        val datePrecedente = LocalDate.of(year, month, 1).minusMonths(1)

        return combine(
            getMonthlyTransactions(month, year),
            getMonthlyTransactions(datePrecedente.monthValue, datePrecedente.year),
            getMonthlyBudget(month, year)
        ) { current, previous, budget ->

            val revenus  = current.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountCents }
            val depenses = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            val depensesPrecedent = previous.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }

            // Top 3 catégories de dépenses, triées par montant décroissant
            val topCategories = current
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .map { (categorie, transactions) ->
                    val total = transactions.sumOf { it.amountCents }
                    CategoryExpense(
                        category    = categorie,
                        totalCents  = total,
                        pourcentage = if (depenses > 0) total.toFloat() / depenses * 100f else 0f
                    )
                }
                .sortedByDescending { it.totalCents }
                .take(3)

            // Devise déduite de la première transaction du mois, EUR par défaut
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
