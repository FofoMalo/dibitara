package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule une projection de trésorerie sur 30 jours.
 *
 * Point de départ : revenus − dépenses réelles du mois courant, recalculés en temps réel
 * depuis les transactions (pas depuis Budget.spentCents qui peut être périmé).
 *
 * Flux pris en compte sur [today, today+30] :
 *   1. Transactions récurrentes EXPENSE → déduites du solde à chaque occurrence
 *   2. Transactions récurrentes INCOME  → ajoutées au solde à chaque occurrence
 *   3. Contributions épargne mensuelles non encore versées → déduites en fin de mois courant
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

    private data class Sources(
        val soldeCourantCents: Long,
        val currency: Currency,
        val recurring: List<Transaction>,
        val savings: List<SavingsAccount>,
        val seuilCents: Long
    )

    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<CashflowProjection> =
        combine(
            budgetRepository.getBudget(today.monthValue, today.year),
            transactionRepository.getRecurring(),
            transactionRepository.getByMonth(today.monthValue, today.year),
            savingsRepository.getAll(),
            userPreferencesRepository.get()
        ) { budget, recurring, monthTransactions, savings, prefs ->
            // Exclure les templates (isRecurring=true) pour ne compter que les transactions réelles.
            // Correction #1 : on ne lit plus Budget.spentCents (potentiellement périmé en base)
            // mais on recalcule le solde en temps réel depuis les transactions du mois.
            val txReelles = monthTransactions.filter { !it.isRecurring }
            val revenusCents  = txReelles.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountCents }
            val depensesCents = txReelles.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            Sources(
                soldeCourantCents = revenusCents - depensesCents,
                currency          = budget?.currency ?: Currency.EUR,
                recurring         = recurring,
                savings           = savings,
                seuilCents        = prefs.seuilFondsCents
            )
        }.transform { sources ->
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
                    soldeCourant    = sources.soldeCourantCents,
                    currency        = sources.currency,
                    recurring       = sources.recurring,
                    pendingContribs = contributionsEnAttente,
                    seuilCents      = sources.seuilCents,
                    today           = today
                )
            )
        }

    private fun buildProjection(
        soldeCourant: Long,
        currency: Currency,
        recurring: List<Transaction>,
        pendingContribs: Long,
        seuilCents: Long,
        today: LocalDate
    ): CashflowProjection {
        val horizon = today.plusDays(30)

        // Flux net par date : valeur positive = entrée d'argent, négative = sortie.
        // Correction #2 : les récurrences INCOME sont désormais incluses (signe positif)
        // et non plus ignorées par un filtre EXPENSE-only.
        val fluxParDate = mutableMapOf<LocalDate, Long>()

        recurring
            .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
            .filter { it.endDate == null || it.endDate >= today }
            .forEach { template ->
                val signe = if (template.type == TransactionType.EXPENSE) -1L else +1L
                occurrencesInRange(template, today, horizon).forEach { date ->
                    fluxParDate[date] = (fluxParDate[date] ?: 0L) + signe * template.amountCents
                }
            }

        // Contributions épargne en attente : sortie d'argent projetée en fin de mois courant
        if (pendingContribs > 0) {
            val finMois = minOf(
                LocalDate.of(today.year, today.monthValue, today.month.length(today.isLeapYear)),
                horizon
            )
            fluxParDate[finMois] = (fluxParDate[finMois] ?: 0L) - pendingContribs
        }

        // Courbe jour par jour : on accumule les flux (le signe est déjà dans la valeur)
        val points = mutableListOf<CashflowPoint>()
        var solde = soldeCourant
        for (i in 0..30) {
            val date = today.plusDays(i.toLong())
            solde += fluxParDate[date] ?: 0L
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
