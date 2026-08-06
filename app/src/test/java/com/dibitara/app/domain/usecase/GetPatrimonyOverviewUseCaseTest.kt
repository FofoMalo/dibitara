package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetPatrimonyOverviewUseCaseTest {

    private val budgetRepo       : BudgetRepository           = mockk()
    private val savingsRepo      : SavingsRepository          = mockk()
    private val investmentRepo   : InvestmentRepository       = mockk()
    private val customInvestRepo : CustomInvestmentRepository = mockk()
    private val debtRepo         : DebtRepository             = mockk()
    private val transactionRepo  : TransactionRepository      = mockk()
    private val prefsRepo        : UserPreferencesRepository  = mockk()
    private val ratesRepo        : ExchangeRateRepository     = mockk()
    private lateinit var useCase : GetPatrimonyOverviewUseCase

    @BeforeEach
    fun setUp() {
        // Devise par défaut EUR → conversion identité dans tous les tests existants
        every { prefsRepo.get() } returns flowOf(UserPreferences())
        every { ratesRepo.getRatesFlow() } returns flowOf(ExchangeRates(1.09, 655.96, 0L))
        useCase = GetPatrimonyOverviewUseCase(
            budgetRepo, savingsRepo, investmentRepo, customInvestRepo,
            debtRepo, transactionRepo, prefsRepo, ratesRepo
        )
    }

    @Test
    fun `calcule le patrimoine net en agrégeant toutes les sources`() = runTest {
        // Budget alloué : 3 000 €. Les dépenses réelles du mois (transactions) : 1 000 €.
        // Résultat attendu : liquidités = 3000 - 1000 = 2000 €
        val budget = Budget(month = 5, year = 2026, allocatedCents = 300000L, spentCents = 0L, currency = Currency.EUR)
        val depense = Transaction(
            amountCents = 100000L, currency = Currency.EUR,
            category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 5, 10)
        )
        val savings = listOf(
            SavingsAccount(type = SavingsType.LIVRET_A, label = "LA", currentBalanceCents = 500000L,
                monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.now())
        )
        val realEstate = listOf(
            RealEstateAsset(label = "Appart", currentValueCents = 20000000L, currency = Currency.EUR, updatedAt = LocalDate.now())
        )
        val scpi = listOf(
            ScpiInvestment(label = "SCPI", sharesCount = 10.0, shareValueCents = 20000L,
                monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.now())
        )
        val airbnb = listOf(
            AirbnbRental(propertyLabel = "Studio", amountCents = 90000L, date = LocalDate.now(), currency = Currency.EUR)
        )
        val vehicule = listOf(
            VehicleRentalEntry(label = "Location weekend", entryType = VehicleEntryType.REVENU, amountCents = 30000L, date = LocalDate.now(), currency = Currency.EUR),
            VehicleRentalEntry(label = "Vidange", entryType = VehicleEntryType.CHARGE, amountCents = 8000L, date = LocalDate.now(), currency = Currency.EUR)
        )
        val debts = listOf(
            Debt(label = "Crédit", totalCents = 5000000L, monthlyPaymentCents = 50000L,
                currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.now())
        )

        every { budgetRepo.getBudget(5, 2026) } returns flowOf(budget)
        every { transactionRepo.getByMonth(5, 2026) } returns flowOf(listOf(depense))
        every { savingsRepo.getAll() } returns flowOf(savings)
        every { investmentRepo.getAllRealEstate() } returns flowOf(realEstate)
        every { investmentRepo.getAllScpi() } returns flowOf(scpi)
        every { investmentRepo.getAirbnbRentalsByYear(2026) } returns flowOf(airbnb)
        every { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(vehicule)
        every { debtRepo.getAll() } returns flowOf(debts)
        every { customInvestRepo.getAllCustomAssets() } returns flowOf(emptyList())
        every { customInvestRepo.getAllEmployeeSavings() } returns flowOf(emptyList())

        val overview = useCase(5, 2026).first()

        // liquidités = budget alloué − dépenses réelles = 300 000 − 100 000 = 200 000
        assertEquals(200000L, overview.liquiditesCents)
        assertEquals(500000L, overview.epargneCents)
        // investissements = immo + scpi = 20 000 000 + 200 000 = 20 200 000
        assertEquals(20200000L, overview.investissementsCents)
        assertEquals(90000L, overview.airbnbAnnualRevenueCents)
        // véhicule locatif net = revenu − charge = 30 000 − 8 000 = 22 000
        assertEquals(22000L, overview.vehicleRentalNetRevenueCents)
        assertEquals(5000000L, overview.dettesTotalCents)
        // patrimoine brut = liquidités + épargne + investissements (Airbnb et véhicule locatif exclus)
        assertEquals(200000L + 500000L + 20200000L, overview.patrimoineBrutCents)
        assertEquals(Currency.EUR, overview.currency)
        // Toutes les sources sont déjà en EUR (devise par défaut) - aucune conversion réelle n'a eu lieu
        assertFalse(overview.hasConvertedValues)
    }

    @Test
    fun `hasConvertedValues est vrai dès qu'une source est dans une autre devise`() = runTest {
        val savingsEnUSD = listOf(
            SavingsAccount(type = SavingsType.LIVRET_A, label = "US Savings", currentBalanceCents = 100000L,
                monthlyContributionCents = 0L, currency = Currency.USD, updatedAt = LocalDate.now())
        )
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(null)
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(emptyList())
        every { savingsRepo.getAll() } returns flowOf(savingsEnUSD)
        every { investmentRepo.getAllRealEstate() } returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() } returns flowOf(emptyList())
        every { investmentRepo.getAirbnbRentalsByYear(any()) } returns flowOf(emptyList())
        every { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(emptyList())
        every { debtRepo.getAll() } returns flowOf(emptyList())
        every { customInvestRepo.getAllCustomAssets() } returns flowOf(emptyList())
        every { customInvestRepo.getAllEmployeeSavings() } returns flowOf(emptyList())

        val overview = useCase(5, 2026).first()

        assertTrue(overview.hasConvertedValues)
    }

    @Test
    fun `les liquidités varient dynamiquement quand une dépense est ajoutée`() = runTest {
        // Scénario BUG-01 : budget défini, puis une dépense est saisie côté transactions.
        // Sans re-sauvegarder le budget, les liquidités doivent diminuer.
        val budget = Budget(month = 5, year = 2026, allocatedCents = 200000L, spentCents = 0L, currency = Currency.EUR)
        val nouvelleDépense = Transaction(
            amountCents = 5000L, currency = Currency.EUR,
            category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 5, 10)
        )

        every { budgetRepo.getBudget(5, 2026) } returns flowOf(budget)
        every { transactionRepo.getByMonth(5, 2026) } returns flowOf(listOf(nouvelleDépense))
        every { savingsRepo.getAll() } returns flowOf(emptyList())
        every { investmentRepo.getAllRealEstate() } returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() } returns flowOf(emptyList())
        every { investmentRepo.getAirbnbRentalsByYear(2026) } returns flowOf(emptyList())
        every { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(emptyList())
        every { debtRepo.getAll() } returns flowOf(emptyList())
        every { customInvestRepo.getAllCustomAssets() } returns flowOf(emptyList())
        every { customInvestRepo.getAllEmployeeSavings() } returns flowOf(emptyList())

        val overview = useCase(5, 2026).first()

        // 2000 € alloués − 50 € dépensés = 1950 € de liquidités
        assertEquals(195000L, overview.liquiditesCents)
    }

    @Test
    fun `utilise EUR et 0 pour les liquidités quand aucun budget n'existe`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(null)
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(emptyList())
        every { savingsRepo.getAll() } returns flowOf(emptyList())
        every { investmentRepo.getAllRealEstate() } returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() } returns flowOf(emptyList())
        every { investmentRepo.getAirbnbRentalsByYear(any()) } returns flowOf(emptyList())
        every { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(emptyList())
        every { debtRepo.getAll() } returns flowOf(emptyList())
        every { customInvestRepo.getAllCustomAssets() } returns flowOf(emptyList())
        every { customInvestRepo.getAllEmployeeSavings() } returns flowOf(emptyList())

        val overview = useCase(5, 2026).first()

        assertEquals(0L, overview.liquiditesCents)
        assertEquals(Currency.EUR, overview.currency)
        assertEquals(0L, overview.patrimoineBrutCents)
    }
}
