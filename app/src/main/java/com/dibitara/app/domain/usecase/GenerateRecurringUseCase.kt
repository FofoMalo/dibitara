package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Génère toutes les occurrences manquantes pour chaque modèle récurrent,
 * de la période suivant la création jusqu'à aujourd'hui.
 *
 * Si l'app n'a pas été ouverte depuis plusieurs semaines/mois, toutes
 * les occurrences intermédiaires sont créées en rattrapage.
 *
 * À appeler au démarrage (via AppViewModel).
 */
class GenerateRecurringUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke() {
        val today = LocalDate.now()
        val templates = repository.getRecurring().first()

        for (template in templates) {
            // Ne génère rien si on a dépassé la date de fin
            if (template.endDate != null && today > template.endDate) continue

            val freq = template.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY
            val base = template.firstPaymentDate ?: template.date
            val cutoff = template.endDate?.let { if (it < today) it else today } ?: today

            when (freq) {
                RecurrenceFrequency.MONTHLY -> generateMonthly(template, base, cutoff)
                RecurrenceFrequency.WEEKLY  -> generateWeekly(template, base, cutoff)
                RecurrenceFrequency.YEARLY  -> generateYearly(template, base, cutoff)
            }
        }
    }

    // ─── MENSUEL ──────────────────────────────────────────────────────────────

    private suspend fun generateMonthly(template: Transaction, base: LocalDate, until: LocalDate) {
        val day = template.recurrenceDay ?: base.dayOfMonth.coerceAtMost(28)

        // Le mois du modèle lui-même compte comme première occurrence — on commence le mois suivant
        var cursor = base.plusMonths(1).withDayOfMonth(1)

        while (!cursor.isAfter(until.withDayOfMonth(1))) {
            val alreadyExists = repository.hasRecurringOccurrenceInRange(
                recurringId = template.id,
                from = cursor,
                to   = cursor.plusMonths(1).minusDays(1)
            )
            if (!alreadyExists) {
                val safeDay = day.coerceAtMost(cursor.month.length(cursor.isLeapYear))
                repository.insert(
                    template.copy(
                        id = 0,
                        date = LocalDate.of(cursor.year, cursor.monthValue, safeDay),
                        isRecurring = false,
                        sourceRecurringId = template.id
                    )
                )
            }
            cursor = cursor.plusMonths(1)
        }
    }

    // ─── HEBDOMADAIRE ─────────────────────────────────────────────────────────

    private suspend fun generateWeekly(template: Transaction, base: LocalDate, until: LocalDate) {
        // La semaine du modèle lui-même est la première occurrence — on commence 7 jours après
        var cursor = base.plusWeeks(1)

        while (!cursor.isAfter(until)) {
            val alreadyExists = repository.hasRecurringOccurrenceInRange(
                recurringId = template.id,
                from = cursor,
                to   = cursor      // On vérifie le jour exact
            )
            if (!alreadyExists) {
                repository.insert(
                    template.copy(
                        id = 0,
                        date = cursor,
                        isRecurring = false,
                        sourceRecurringId = template.id
                    )
                )
            }
            cursor = cursor.plusWeeks(1)
        }
    }

    // ─── ANNUEL ───────────────────────────────────────────────────────────────

    private suspend fun generateYearly(template: Transaction, base: LocalDate, until: LocalDate) {
        var year = base.year + 1   // L'année du modèle est la première occurrence

        while (year <= until.year) {
            val occurrenceDate = safeYearlyDate(base.monthValue, base.dayOfMonth, year)

            if (!occurrenceDate.isAfter(until)) {
                val alreadyExists = repository.hasRecurringOccurrenceInRange(
                    recurringId = template.id,
                    from = LocalDate.of(year, 1, 1),
                    to   = LocalDate.of(year, 12, 31)
                )
                if (!alreadyExists) {
                    repository.insert(
                        template.copy(
                            id = 0,
                            date = occurrenceDate,
                            isRecurring = false,
                            sourceRecurringId = template.id
                        )
                    )
                }
            }
            year++
        }
    }

    // Gère les cas limites (ex. 29 févr. sur une année non-bissextile)
    private fun safeYearlyDate(month: Int, day: Int, year: Int): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        LocalDate.of(year, month, 28)
    }
}
