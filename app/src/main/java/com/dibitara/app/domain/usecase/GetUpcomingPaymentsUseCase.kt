package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.UpcomingPayment
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Calcule les prochaines échéances à venir pour tous les modèles récurrents actifs.
 *
 * [limit] — nombre maximum de résultats retournés (défaut 5).
 * Les résultats sont triés par date croissante.
 */
class GetUpcomingPaymentsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 5): Flow<List<UpcomingPayment>> =
        repository.getRecurring().map { templates ->
            val today = LocalDate.now()
            templates
                .filter { template ->
                    // Exclure les modèles dont la date de fin est dépassée
                    template.endDate == null || template.endDate >= today
                }
                .mapNotNull { template ->
                    val nextDate = nextOccurrenceAfter(template, today) ?: return@mapNotNull null
                    // Vérifier que la date calculée ne dépasse pas la date de fin
                    if (template.endDate != null && nextDate > template.endDate) return@mapNotNull null
                    UpcomingPayment(template = template, nextDate = nextDate)
                }
                .sortedBy { it.nextDate }
                .take(limit)
        }

    /**
     * Calcule la prochaine date d'occurrence strictement après [after].
     * Retourne null si la fréquence est inconnue.
     */
    private fun nextOccurrenceAfter(template: Transaction, after: LocalDate): LocalDate? {
        val freq = template.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY
        val base = template.firstPaymentDate ?: template.date

        return when (freq) {
            RecurrenceFrequency.MONTHLY -> nextMonthly(template, after)
            RecurrenceFrequency.WEEKLY  -> nextWeekly(base.dayOfWeek, after)
            RecurrenceFrequency.YEARLY  -> nextYearly(base, after)
        }
    }

    private fun nextMonthly(template: Transaction, after: LocalDate): LocalDate {
        val base = template.firstPaymentDate ?: template.date
        val day = (template.recurrenceDay ?: base.dayOfMonth).coerceAtMost(28)

        // Candidat dans le mois courant
        val thisMonth = LocalDate.of(
            after.year, after.monthValue,
            day.coerceAtMost(after.month.length(after.isLeapYear))
        )
        if (thisMonth > after) return thisMonth

        // Sinon, premier jour éligible du mois suivant
        val next = after.plusMonths(1)
        return LocalDate.of(
            next.year, next.monthValue,
            day.coerceAtMost(next.month.length(next.isLeapYear))
        )
    }

    private fun nextWeekly(dayOfWeek: DayOfWeek, after: LocalDate): LocalDate {
        // TemporalAdjusters.next() donne le prochain jour de semaine STRICT (exclu today)
        val candidate = after.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return if (candidate > after) candidate else candidate.plusWeeks(1)
    }

    private fun nextYearly(base: LocalDate, after: LocalDate): LocalDate {
        val candidate = safeDate(after.year, base.monthValue, base.dayOfMonth)
        return if (candidate > after) candidate
        else safeDate(after.year + 1, base.monthValue, base.dayOfMonth)
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        LocalDate.of(year, month, 28)
    }
}
