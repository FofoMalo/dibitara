package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.PreciousMetalAsset
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Agrège toutes les sources pour calculer la vue patrimoniale.
 *
 * Sprint 23 : chaque actif est converti vers la devise par défaut de l'utilisateur
 * avant la sommation, grâce aux taux de change mis en cache dans DataStore.
 *
 * Structure des combines (combine() est limité à 5 flows) :
 *   Groupe A : budget + épargne + immo + transactions du mois
 *   Groupe B : SCPI + Airbnb + véhicule locatif + dettes
 *   Groupe C : investissements personnalisés (métaux, actifs libres, épargne salariale)
 *   innerFlow = combine(A, B, C) → [RawOverview] (données brutes, sans conversion)
 *   groupD    = combine(préférences, taux de change)
 *   Résultat  = combine(innerFlow, groupD) → PatrimonyOverview converti
 */
class GetPatrimonyOverviewUseCase @Inject constructor(
    private val budgetRepository           : BudgetRepository,
    private val savingsRepository          : SavingsRepository,
    private val investmentRepository       : InvestmentRepository,
    private val customInvestmentRepository : CustomInvestmentRepository,
    private val debtRepository             : DebtRepository,
    private val transactionRepository      : TransactionRepository,
    private val userPreferencesRepository  : UserPreferencesRepository,
    private val exchangeRateRepository     : ExchangeRateRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<PatrimonyOverview> {

        val groupA = combine(
            budgetRepository.getBudget(month, year),
            savingsRepository.getAll(),
            investmentRepository.getAllRealEstate(),
            transactionRepository.getByMonth(month, year)
        ) { budget, savings, realEstate, transactions ->
            GroupA(budget, savings, realEstate, transactions)
        }

        val groupB = combine(
            investmentRepository.getAllScpi(),
            investmentRepository.getAirbnbRentalsByYear(year),
            investmentRepository.getAllVehicleRentalEntries(),
            debtRepository.getAll()
        ) { scpi, airbnb, vehicle, debts -> GroupB(scpi, airbnb, vehicle, debts) }

        val groupC = combine(
            customInvestmentRepository.getAllPreciousMetals(),
            customInvestmentRepository.getAllCustomAssets(),
            customInvestmentRepository.getAllEmployeeSavings()
        ) { metals, assets, empSavings -> GroupC(metals, assets, empSavings) }

        // Regroupe A, B, C sans encore convertir les devises
        val innerFlow = combine(groupA, groupB, groupC) { a, b, c -> RawOverview(a, b, c) }

        val groupD = combine(
            userPreferencesRepository.get(),
            exchangeRateRepository.getRatesFlow()
        ) { prefs, rates -> prefs.deviseParDefaut to rates }

        return combine(innerFlow, groupD) { raw, (targetCurrency, rates) ->
            fun Long.cvt(from: Currency) =
                CurrencyConverter.convertCents(this, from, targetCurrency, rates)

            val depensesDuMois = raw.a.transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountCents.cvt(it.currency) }
            val budgetAlloue = raw.a.budget?.let { it.allocatedCents.cvt(it.currency) } ?: 0L

            PatrimonyOverview(
                liquiditesCents          = budgetAlloue - depensesDuMois,
                epargneCents             = raw.a.savings.sumOf    { it.currentBalanceCents.cvt(it.currency) },
                investissementsCents     =
                    raw.a.realEstate.sumOf  { it.currentValueCents.cvt(it.currency) }  +
                    raw.b.scpi.sumOf        { it.totalValueCents.cvt(it.currency) }    +
                    raw.c.metals.sumOf      { it.totalValueCents.cvt(it.currency) }    +
                    raw.c.assets.sumOf      { it.totalValueCents.cvt(it.currency) }    +
                    raw.c.empSavings.sumOf  { it.currentBalanceCents.cvt(it.currency) },
                airbnbAnnualRevenueCents = raw.b.airbnb.sumOf { it.amountCents.cvt(it.currency) },
                vehicleRentalNetRevenueCents = raw.b.vehicle.sumOf { entry ->
                    val cents = entry.amountCents.cvt(entry.currency)
                    if (entry.entryType == VehicleEntryType.REVENU) cents else -cents
                },
                dettesTotalCents         = raw.b.debts.sumOf  { it.totalCents.cvt(it.currency) },
                currency                 = targetCurrency
            )
        }
    }

    // ─── Holders internes pour contourner la limite de 5 args de combine ─────

    private data class GroupA(
        val budget       : Budget?,
        val savings      : List<SavingsAccount>,
        val realEstate   : List<RealEstateAsset>,
        val transactions : List<Transaction>
    )

    private data class GroupB(
        val scpi    : List<ScpiInvestment>,
        val airbnb  : List<AirbnbRental>,
        val vehicle : List<VehicleRentalEntry>,
        val debts   : List<Debt>
    )

    private data class GroupC(
        val metals     : List<PreciousMetalAsset>,
        val assets     : List<CustomAsset>,
        val empSavings : List<EmployeeSavings>
    )

    private data class RawOverview(val a: GroupA, val b: GroupB, val c: GroupC)
}
