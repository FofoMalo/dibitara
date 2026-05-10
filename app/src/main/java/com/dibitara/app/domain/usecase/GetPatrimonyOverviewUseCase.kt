package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Agrège toutes les sources pour calculer la vue patrimoniale.
 * combine() est limité à 5 flows — on imbrique les combines pour gérer 7 sources.
 *
 * Structure :
 *   Groupe A : budget + épargne + immo + transactions du mois
 *   Groupe B : SCPI + Airbnb + dettes
 *   Résultat : combine(groupA, groupB)
 */
class GetPatrimonyOverviewUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val savingsRepository: SavingsRepository,
    private val investmentRepository: InvestmentRepository,
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<PatrimonyOverview> {
        // Groupe A : budget + épargne + immo + transactions réelles du mois
        // Les transactions permettent de calculer les liquidités dynamiquement
        // (sans attendre que l'utilisateur re-sauvegarde son budget en base)
        val groupA = combine(
            budgetRepository.getBudget(month, year),
            savingsRepository.getAll(),
            investmentRepository.getAllRealEstate(),
            transactionRepository.getByMonth(month, year)
        ) { budget, savings, realEstate, transactions ->
            Triple(budget, savings, realEstate) to transactions
        }

        // Groupe B : SCPI + Airbnb + dettes
        val groupB = combine(
            investmentRepository.getAllScpi(),
            investmentRepository.getAirbnbRentalsByYear(year),
            debtRepository.getAll()
        ) { scpi, airbnb, debts ->
            Triple(scpi, airbnb, debts)
        }

        return combine(groupA, groupB) { (groupATriple, transactions), (scpi, airbnb, debts) ->
            val (budget, savings, realEstate) = groupATriple

            // Liquidités = budget alloué − dépenses réelles du mois
            // On calcule depuis les transactions plutôt que depuis budget.spentCents (figé en base)
            val depensesDuMois = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountCents }
            val liquidites = (budget?.allocatedCents ?: 0L) - depensesDuMois

            PatrimonyOverview(
                liquiditesCents          = liquidites,
                epargneCents             = savings.sumOf { it.currentBalanceCents },
                investissementsCents     = realEstate.sumOf { it.currentValueCents } +
                                           scpi.sumOf { it.totalValueCents },
                airbnbAnnualRevenueCents = airbnb.sumOf { it.amountCents },
                dettesTotalCents         = debts.sumOf { it.totalCents },
                currency                 = budget?.currency ?: Currency.EUR
            )
        }
    }
}
