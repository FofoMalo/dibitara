package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryTrend
import com.dibitara.app.domain.model.MonthlyAmount
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule les tendances de dépenses par catégorie sur les 6 derniers mois.
 *
 * Retourne au maximum les 8 catégories les plus dépensières, ordonnées par
 * [CategoryTrend.totalSixMoisCents] décroissant. Les catégories dont toutes
 * les 6 valeurs sont nulles sont exclues.
 */
class GetCategoryTrendsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<CategoryTrend>> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.withDayOfMonth(1).minusMonths(5)

        return transactionRepository.getByDateRange(sixMonthsAgo, today).map { transactions ->
            // On ne considère que les dépenses
            val depenses = transactions.filter { it.type == TransactionType.EXPENSE }

            // Regroupement par (catégorie, mois, année)
            val grouped = depenses.groupBy {
                Triple(it.category, it.date.monthValue, it.date.year)
            }.mapValues { (_, txs) -> txs.sumOf { it.amountCents } }

            // Construire la liste des 6 mois (du plus ancien au plus récent)
            val sixMois = (5 downTo 0).map { offset ->
                today.withDayOfMonth(1).minusMonths(offset.toLong())
            }

            // Toutes les catégories présentes dans les dépenses
            val categories = depenses.map { it.category }.distinct()

            categories.mapNotNull { category ->
                val moisData = sixMois.map { date ->
                    MonthlyAmount(
                        month = date.monthValue,
                        year = date.year,
                        totalCents = grouped[Triple(category, date.monthValue, date.year)] ?: 0L
                    )
                }

                val total = moisData.sumOf { it.totalCents }
                if (total == 0L) return@mapNotNull null  // Exclure les catégories vides sur toute la période

                // Variation entre le dernier mois et l'avant-dernier
                val lastMonth = moisData.last().totalCents
                val prevMonth = moisData[moisData.size - 2].totalCents
                val variationPct = if (prevMonth != 0L) {
                    ((lastMonth - prevMonth) * 100L / prevMonth).toInt()
                } else null

                CategoryTrend(
                    category           = category,
                    moisData           = moisData,
                    totalSixMoisCents  = total,
                    variationPct       = variationPct
                )
            }
                .sortedByDescending { it.totalSixMoisCents }
                .take(8)
        }
    }
}
