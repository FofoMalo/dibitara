package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Retourne le total des dépenses (EXPENSE) pour chacun des 6 derniers mois.
 * Le mois courant est inclus ; les mois sans dépense ont totalCents = 0.
 * La liste est ordonnée du plus ancien au plus récent.
 */
class GetSpendingHistoryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<MonthlyExpense>> {
        val today = LocalDate.now()
        // Premier jour du mois d'il y a 5 mois (6 mois au total avec le mois courant)
        val from = today.withDayOfMonth(1).minusMonths(5)

        return repository.getByDateRange(from, today).map { transactions ->
            // On regroupe les dépenses par (mois, année)
            val grouped = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { Pair(it.date.monthValue, it.date.year) }

            // On construit les 6 entrées même si certains mois sont vides
            (5 downTo 0).map { offset ->
                val date = today.withDayOfMonth(1).minusMonths(offset.toLong())
                MonthlyExpense(
                    month = date.monthValue,
                    year = date.year,
                    totalCents = grouped[Pair(date.monthValue, date.year)]
                        ?.sumOf { it.amountCents } ?: 0L
                )
            }
        }
    }
}
