package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Agrège toutes les sources pour calculer la vue patrimoniale.
 * combine() est limité à 5 flows — on imbrique deux combine() pour gérer 6 sources.
 */
class GetPatrimonyOverviewUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val savingsRepository: SavingsRepository,
    private val investmentRepository: InvestmentRepository,
    private val debtRepository: DebtRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<PatrimonyOverview> {
        // Groupe A : budget + épargne + immo
        val groupA = combine(
            budgetRepository.getBudget(month, year),
            savingsRepository.getAll(),
            investmentRepository.getAllRealEstate()
        ) { budget, savings, realEstate ->
            Triple(budget, savings, realEstate)
        }

        // Groupe B : SCPI + Airbnb + dettes
        val groupB = combine(
            investmentRepository.getAllScpi(),
            investmentRepository.getAirbnbRentalsByYear(year),
            debtRepository.getAll()
        ) { scpi, airbnb, debts ->
            Triple(scpi, airbnb, debts)
        }

        return combine(groupA, groupB) { (budget, savings, realEstate), (scpi, airbnb, debts) ->
            PatrimonyOverview(
                liquiditesCents = budget?.remainingCents ?: 0L,
                epargneCents = savings.sumOf { it.currentBalanceCents },
                investissementsCents = realEstate.sumOf { it.currentValueCents } +
                        scpi.sumOf { it.totalValueCents },
                airbnbAnnualRevenueCents = airbnb.sumOf { it.amountCents },
                dettesTotalCents = debts.sumOf { it.totalCents },
                currency = budget?.currency ?: Currency.EUR
            )
        }
    }
}
