package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule une projection de trésorerie sur 30 jours.
 *
 * Point de départ : [Budget.remainingCents] du mois courant (revenus - dépenses déjà saisies).
 * Engagements déduits :
 *   1. Paiements récurrents de type EXPENSE dont la prochaine occurrence tombe dans [today, today+30]
 *   2. Contributions épargne mensuelles ([SavingsAccount.monthlyContributionCents]) non encore
 *      versées ce mois-ci — placées en fin de mois courant dans la projection.
 *
 * [today] est injectable pour permettre les tests sans mocker LocalDate.now().
 */
class GetCashflowProjectionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val savingsRepository: SavingsRepository,
    private val versementRepository: VersementRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    // Agrège les 4 flows réactifs en un seul objet intermédiaire pour le transform
    private data class Sources(
        val budget: Budget?,
        val recurring: List<Transaction>,
        val savings: List<SavingsAccount>,
        val seuilCents: Long
    )

    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<CashflowProjection> =
        combine(
            budgetRepository.getBudget(today.monthValue, today.year),
            transactionRepository.getRecurring(),
            savingsRepository.getAll(),
            userPreferencesRepository.get()
        ) { budget, recurring, savings, prefs ->
            Sources(budget, recurring, savings, prefs.seuilFondsCents)
        }.transform { sources ->
            // transform est suspend-aware : on peut appeler versementRepository.existsPourMois ici
            val contributionsEnAttente = sources.savings
                .filter { it.monthlyContributionCents > 0 }
                .filter { account ->
                    !versementRepository.existsPourMois(
                        account.id, CompteType.EPARGNE,
                        today.year, today.monthValue
                    )
                }
                .sumOf { it.monthlyContributionCents }

            emit(
                buildProjection(
                    budget              = sources.budget,
                    recurring           = sources.recurring,
                    pendingContribs     = contributionsEnAttente,
                    seuilCents          = sources.seuilCents,
                    today               = today
                )
            )
        }

    private fun buildProjection(
        budget: Budget?,
        recurring: List<Transaction>,
        pendingContribs: Long,
        seuilCents: Long,
        today: LocalDate
    ): CashflowProjection {
        val soldeCourant = budget?.remainingCents ?: 0L
        val currency = budget?.currency ?: Currency.EUR
        val horizon = today.plusDays(30)

        // Regrouper les montants à déduire par date
        val chargesParDate = mutableMapOf<LocalDate, Long>()

        recurring
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.endDate == null || it.endDate >= today }
            .forEach { template ->
                occurrencesInRange(template, today, horizon).forEach { date ->
                    chargesParDate[date] = (chargesParDate[date] ?: 0L) + template.amountCents
                }
            }

        // Les contributions épargne en attente sont projetées en fin de mois courant
        if (pendingContribs > 0) {
            val finMois = minOf(
                LocalDate.of(today.year, today.monthValue, today.month.length(today.isLeapYear)),
                horizon
            )
            chargesParDate[finMois] = (chargesParDate[finMois] ?: 0L) + pendingContribs
        }

        // Construire la courbe jour par jour
        val points = mutableListOf<CashflowPoint>()
        var solde = soldeCourant
        for (i in 0..30) {
            val date = today.plusDays(i.toLong())
            solde -= chargesParDate[date] ?: 0L
            points.add(CashflowPoint(date, solde))
        }

        return CashflowProjection(
            soldeActuelCents        = soldeCourant,
            soldeProjecte30jCents   = points.last().soldeCents,
            jourPassageSeuilNegatif = points.firstOrNull { it.soldeCents < seuilCents }?.date,
            pointsTimeline          = points,
            currency                = currency
        )
    }

    /**
     * Retourne toutes les dates d'occurrence de [template] dans la fenêtre [[from], [to]] incluse.
     * Interne mais visible pour les tests unitaires.
     */
    internal fun occurrencesInRange(
        template: Transaction,
        from: LocalDate,
        to: LocalDate
    ): List<LocalDate> {
        val freq = template.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY
        val results = mutableListOf<LocalDate>()
        var current: LocalDate? = premiereOccurrenceApresOuEgal(template, from, freq)
        while (current != null && current <= to) {
            if (template.endDate == null || current <= template.endDate) results.add(current)
            current = prochaineOccurrenceApres(template, current, freq)
        }
        return results
    }

    private fun premiereOccurrenceApresOuEgal(
        template: Transaction,
        from: LocalDate,
        freq: RecurrenceFrequency
    ): LocalDate {
        val base = template.firstPaymentDate ?: template.date
        return when (freq) {
            RecurrenceFrequency.MONTHLY -> {
                val jour = (template.recurrenceDay ?: base.dayOfMonth).coerceAtMost(28)
                val candidat = LocalDate.of(
                    from.year, from.monthValue,
                    jour.coerceAtMost(from.month.length(from.isLeapYear))
                )
                if (candidat >= from) candidat
                else {
                    val m = from.plusMonths(1)
                    LocalDate.of(m.year, m.monthValue, jour.coerceAtMost(m.month.length(m.isLeapYear)))
                }
            }
            RecurrenceFrequency.WEEKLY -> {
                var c = from
                while (c.dayOfWeek != base.dayOfWeek) c = c.plusDays(1)
                c
            }
            RecurrenceFrequency.YEARLY -> {
                val c = safeDate(from.year, base.monthValue, base.dayOfMonth)
                if (c >= from) c else safeDate(from.year + 1, base.monthValue, base.dayOfMonth)
            }
        }
    }

    private fun prochaineOccurrenceApres(
        template: Transaction,
        after: LocalDate,
        freq: RecurrenceFrequency
    ): LocalDate {
        val base = template.firstPaymentDate ?: template.date
        return when (freq) {
            RecurrenceFrequency.MONTHLY -> {
                val jour = (template.recurrenceDay ?: base.dayOfMonth).coerceAtMost(28)
                val m = after.plusMonths(1)
                LocalDate.of(m.year, m.monthValue, jour.coerceAtMost(m.month.length(m.isLeapYear)))
            }
            RecurrenceFrequency.WEEKLY  -> after.plusWeeks(1)
            RecurrenceFrequency.YEARLY  -> safeDate(after.year + 1, base.monthValue, base.dayOfMonth)
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        LocalDate.of(year, month, 28)
    }
}
