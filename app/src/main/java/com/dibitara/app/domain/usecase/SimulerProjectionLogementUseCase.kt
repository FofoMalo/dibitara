package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BudgetBucket
import com.dibitara.app.domain.model.CashflowPoint
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.ScenarioLogementProjection
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.bucket
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Projette le reste à vivre cumulé sur plusieurs mois sous un scénario logement simulé.
 *
 * Le revenu simulé et les besoins incompressibles sont tenus constants d'un mois à l'autre -
 * seuls deux évènements réels, déjà présents dans les données, font varier la trajectoire :
 * une dette (hors crédit du bien) qui se termine dans la fenêtre, et une charge annuelle
 * récurrente en catégorie BESOINS dont l'échéance tombe dans la fenêtre.
 * Voir [ScenarioLogementProjection].
 */
class SimulerProjectionLogementUseCase @Inject constructor(
    private val getDebts             : GetDebtsUseCase,
    private val getSavings           : GetSavingsUseCase,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepo  : UserPreferencesRepository,
    private val exchangeRateRepo     : ExchangeRateRepository
) {
    operator fun invoke(
        maisonDebtId                : Long?,
        revenuSimuleCents           : Long,
        besoinsIncompressiblesCents : Long,
        horizonMois                 : Int = 6,
        today                       : LocalDate = LocalDate.now()
    ): Flow<ScenarioLogementProjection> {
        val conversionFlow = combine(
            userPreferencesRepo.get(),
            exchangeRateRepo.getRatesFlow()
        ) { prefs, rates -> prefs to rates }

        return combine(
            getDebts(),
            getSavings(),
            transactionRepository.getRecurring(),
            conversionFlow
        ) { dettes, comptes, recurring, (prefs, rates) ->

            val devise = prefs.deviseParDefaut
            fun Long.cvt(from: Currency) = CurrencyConverter.convertCents(this, from, devise, rates)

            val autresDettes = dettes.filter { it.id != maisonDebtId && it.monthlyPaymentCents > 0 }
            val contributionsEpargneCents = comptes.sumOf { it.monthlyContributionCents.cvt(it.currency) }

            // Charges annuelles récurrentes en bucket BESOINS dont la prochaine échéance
            // tombe dans la fenêtre projetée.
            val horizonDate = today.plusMonths(horizonMois.toLong())
            val chargesAnnuelles = recurring
                .filter { it.type == TransactionType.EXPENSE }
                .filter { it.recurrenceFrequency == RecurrenceFrequency.YEARLY }
                .filter { it.category.bucket == BudgetBucket.BESOINS }
                .filter { it.endDate == null || !it.endDate.isBefore(today) }
                .mapNotNull { modele ->
                    val base = modele.firstPaymentDate ?: modele.date
                    var prochaine = dateSure(today.year, base.monthValue, base.dayOfMonth)
                    if (prochaine.isBefore(today)) prochaine = dateSure(today.year + 1, base.monthValue, base.dayOfMonth)
                    if (!prochaine.isAfter(horizonDate)) prochaine to modele.amountCents.cvt(modele.currency) else null
                }

            val points = mutableListOf<CashflowPoint>()
            var cumul = 0L
            var moisNegatif: LocalDate? = null

            for (i in 0 until horizonMois) {
                val moisDebut = today.plusMonths(i.toLong()).withDayOfMonth(1)
                val moisFin   = moisDebut.plusMonths(1).minusDays(1)

                val dettesActives = autresDettes.filter { debt ->
                    val nbMoisRestants = (debt.totalCents / debt.monthlyPaymentCents).toInt()
                    i < nbMoisRestants
                }
                val engagementsMois = dettesActives.sumOf { it.monthlyPaymentCents.cvt(it.currency) } +
                    contributionsEpargneCents

                val chargeAnnuelleDuMois = chargesAnnuelles
                    .filter { (date, _) -> !date.isBefore(moisDebut) && !date.isAfter(moisFin) }
                    .sumOf { it.second }

                val netMois = revenuSimuleCents - besoinsIncompressiblesCents - chargeAnnuelleDuMois - engagementsMois
                cumul += netMois
                if (moisNegatif == null && cumul < 0) moisNegatif = moisFin
                points.add(CashflowPoint(moisFin, cumul))
            }

            ScenarioLogementProjection(
                points             = points,
                currency           = devise,
                moisPassageNegatif = moisNegatif
            )
        }
    }

    private fun dateSure(year: Int, month: Int, day: Int): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        LocalDate.of(year, month, 28)
    }
}
