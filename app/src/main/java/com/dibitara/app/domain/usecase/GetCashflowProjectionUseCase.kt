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
 *   4. Contributions SCPI mensuelles non encore versées → déduites en fin de mois courant
 *   5. Mensualités de crédit → déduites en fin de chaque mois dans la fenêtre
 *
 * [today] est injectable pour permettre les tests sans mocker LocalDate.now().
 */
class GetCashflowProjectionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val savingsRepository: SavingsRepository,
    private val investmentRepository: InvestmentRepository,
    private val debtRepository: DebtRepository,
    private val versementRepository: VersementRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private data class Sources(
        val soldeCourantCents: Long,
        val currency: Currency,
        val recurring: List<Transaction>,
        val savings: List<SavingsAccount>,
        val scpi: List<ScpiInvestment>,
        val debts: List<Debt>,
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
            val txReelles    = monthTransactions.filter { !it.isRecurring }
            val revenusCents  = txReelles.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountCents }
            val depensesCents = txReelles.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            Sources(
                soldeCourantCents = revenusCents - depensesCents,
                currency          = budget?.currency ?: Currency.EUR,
                recurring         = recurring,
                savings           = savings,
                scpi              = emptyList(),
                debts             = emptyList(),
                seuilCents        = prefs.seuilFondsCents
            )
        }.combine(
            combine(investmentRepository.getAllScpi(), debtRepository.getAll()) { scpi, debts -> scpi to debts }
        ) { sources, (scpi, debts) ->
            sources.copy(scpi = scpi, debts = debts)
        }.transform { sources ->

            val epargneNonVersee = sources.savings
                .filter { it.monthlyContributionCents > 0 }
                .filter { account ->
                    !versementRepository.existsPourMois(
                        account.id, CompteType.EPARGNE, today.year, today.monthValue
                    )
                }

            // Même logique que l'épargne : contributions SCPI non encore versées ce mois
            val scpiNonVersee = sources.scpi
                .filter { it.monthlyContributionCents > 0 }
                .filter { scpi ->
                    !versementRepository.existsPourMois(
                        scpi.id, CompteType.SCPI, today.year, today.monthValue
                    )
                }

            emit(
                buildProjection(
                    soldeCourant     = sources.soldeCourantCents,
                    currency         = sources.currency,
                    recurring        = sources.recurring,
                    epargneNonVersee = epargneNonVersee,
                    scpiNonVersee    = scpiNonVersee,
                    dettes           = sources.debts,
                    seuilCents       = sources.seuilCents,
                    today            = today
                )
            )
        }

    private fun buildProjection(
        soldeCourant: Long,
        currency: Currency,
        recurring: List<Transaction>,
        epargneNonVersee: List<SavingsAccount>,
        scpiNonVersee: List<ScpiInvestment>,
        dettes: List<Debt>,
        seuilCents: Long,
        today: LocalDate
    ): CashflowProjection {
        val horizon = today.plusDays(30)
        val fluxParDate = mutableMapOf<LocalDate, Long>()
        val evenements  = mutableListOf<EventProjecte>()

        recurring
            .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
            .filter { it.endDate == null || it.endDate >= today }
            .forEach { template ->
                val signe = if (template.type == TransactionType.EXPENSE) -1L else +1L
                val sensFlux = if (template.type == TransactionType.EXPENSE) SensFlux.SORTIE else SensFlux.ENTREE
                occurrencesInRange(template, today, horizon).forEach { date ->
                    fluxParDate[date] = (fluxParDate[date] ?: 0L) + signe * template.amountCents
                    evenements.add(
                        EventProjecte(
                            date         = date,
                            label        = template.note.ifBlank { "Transaction récurrente" },
                            montantCents = template.amountCents,
                            sens         = sensFlux
                        )
                    )
                }
            }

        // Contributions épargne non versées → fin du mois courant
        val finMoisCourant = minOf(dernierJourDuMois(today), horizon)
        epargneNonVersee.forEach { savings ->
            fluxParDate[finMoisCourant] = (fluxParDate[finMoisCourant] ?: 0L) - savings.monthlyContributionCents
            evenements.add(
                EventProjecte(
                    date         = finMoisCourant,
                    label        = "Versement ${savings.label}",
                    montantCents = savings.monthlyContributionCents,
                    sens         = SensFlux.SORTIE
                )
            )
        }

        // Contributions SCPI non versées → fin du mois courant
        scpiNonVersee.forEach { scpi ->
            fluxParDate[finMoisCourant] = (fluxParDate[finMoisCourant] ?: 0L) - scpi.monthlyContributionCents
            evenements.add(
                EventProjecte(
                    date         = finMoisCourant,
                    label        = "Versement SCPI ${scpi.label}",
                    montantCents = scpi.monthlyContributionCents,
                    sens         = SensFlux.SORTIE
                )
            )
        }

        // Mensualités crédit : une occurrence par mois dans la fenêtre [today, horizon].
        // Si paymentDay est renseigné on l'utilise, sinon on place en fin de mois.
        dettes.filter { it.monthlyPaymentCents > 0 }.forEach { debt ->
            var debutMois = today.withDayOfMonth(1)
            while (debutMois <= horizon) {
                val lastDayOfMonth = debutMois.month.length(debutMois.isLeapYear)
                val finMois = minOf(dernierJourDuMois(debutMois), horizon)
                val datePaiement = if (debt.paymentDay != null) {
                    val jour = debt.paymentDay.coerceAtMost(lastDayOfMonth)
                    val candidate = java.time.LocalDate.of(debutMois.year, debutMois.monthValue, jour)
                    minOf(candidate, horizon)
                } else {
                    finMois
                }
                if (datePaiement >= today) {
                    fluxParDate[datePaiement] = (fluxParDate[datePaiement] ?: 0L) - debt.monthlyPaymentCents
                    evenements.add(
                        EventProjecte(
                            date         = datePaiement,
                            label        = "Mensualité ${debt.label}",
                            montantCents = debt.monthlyPaymentCents,
                            sens         = SensFlux.SORTIE
                        )
                    )
                }
                debutMois = debutMois.plusMonths(1)
            }
        }

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
            currency                = currency,
            evenementsAVenir        = evenements.sortedBy { it.date }
        )
    }

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
                // Correction #8 : on utilise la longueur réelle du mois cible au lieu du plafond fixe 28.
                // Un prélèvement le 30 reste au 30 dans les mois de 30/31 jours,
                // et est ramené au 28/29 en février seulement.
                val jourSouhaite = template.recurrenceDay ?: base.dayOfMonth
                val jourEffectif = jourSouhaite.coerceAtMost(from.month.length(from.isLeapYear))
                val candidat = LocalDate.of(from.year, from.monthValue, jourEffectif)
                if (candidat >= from) candidat
                else {
                    val m = from.plusMonths(1)
                    LocalDate.of(m.year, m.monthValue, jourSouhaite.coerceAtMost(m.month.length(m.isLeapYear)))
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
                val jourSouhaite = template.recurrenceDay ?: base.dayOfMonth
                val m = after.plusMonths(1)
                LocalDate.of(m.year, m.monthValue, jourSouhaite.coerceAtMost(m.month.length(m.isLeapYear)))
            }
            RecurrenceFrequency.WEEKLY  -> after.plusWeeks(1)
            RecurrenceFrequency.YEARLY  -> safeDate(after.year + 1, base.monthValue, base.dayOfMonth)
        }
    }

    private fun dernierJourDuMois(date: LocalDate): LocalDate =
        LocalDate.of(date.year, date.month, date.month.length(date.isLeapYear))

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        LocalDate.of(year, month, 28)
    }
}
