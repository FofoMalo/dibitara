package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryTrend
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.MonthlyAmount
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule les tendances de dépenses par catégorie sur les 6 derniers mois.
 *
 * Retourne au maximum les 8 catégories les plus dépensières, ordonnées par
 * [CategoryTrend.totalSixMoisCents] décroissant. Les catégories dont toutes
 * les 6 valeurs sont nulles sont exclues.
 *
 * Chaque montant est converti vers [com.dibitara.app.domain.model.UserPreferences.deviseParDefaut]
 * avant sommation, comme pour le rapport mensuel et le patrimoine.
 */
class GetCategoryTrendsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    operator fun invoke(): Flow<List<CategoryTrend>> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.withDayOfMonth(1).minusMonths(5)

        return combine(
            transactionRepository.getByDateRange(sixMonthsAgo, today),
            userPreferencesRepository.get(),
            exchangeRateRepository.getRatesFlow()
        ) { transactions, prefs, rates ->
            val target = prefs.deviseParDefaut
            fun Long.cvt(from: Currency) = CurrencyConverter.convertCents(this, from, target, rates)

            // On ne considère que les dépenses
            val depenses = transactions.filter { it.type == TransactionType.EXPENSE }

            // Regroupement par (catégorie, mois, année)
            val grouped = depenses.groupBy {
                Triple(it.category, it.date.monthValue, it.date.year)
            }.mapValues { (_, txs) -> txs.sumOf { it.amountCents.cvt(it.currency) } }

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
                    variationPct       = variationPct,
                    currency           = target
                )
            }
                .sortedByDescending { it.totalSixMoisCents }
                .take(8)
        }
    }
}
