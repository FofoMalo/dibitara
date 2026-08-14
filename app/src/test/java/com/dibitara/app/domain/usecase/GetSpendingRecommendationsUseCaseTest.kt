package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
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

class GetSpendingRecommendationsUseCaseTest {

    private val transactionRepo     : TransactionRepository       = mockk()
    private val debtRepo            : DebtRepository              = mockk()
    private val savingsRepo         : SavingsRepository           = mockk()
    private val categoryEnvelopeRepo: CategoryEnvelopeRepository  = mockk()
    private val prefsRepo           : UserPreferencesRepository   = mockk()
    private val exchangeRateRepo    : ExchangeRateRepository      = mockk()

    private val useCase = GetSpendingRecommendationsUseCase(
        getMonthlyTransactions      = GetMonthlyTransactionsUseCase(transactionRepo),
        getDebts                    = GetDebtsUseCase(debtRepo),
        getSavings                  = GetSavingsUseCase(savingsRepo),
        getCategoryEnvelopes        = GetCategoryEnvelopesUseCase(categoryEnvelopeRepo),
        userPreferencesRepo         = prefsRepo,
        exchangeRateRepo            = exchangeRateRepo,
        identifierVirementsInternes = IdentifierVirementsInternesUseCase()
    )

    // refMonth=5, refYear=2026 → 3 mois analysés : avril, mars, février 2026
    private val rates = ExchangeRates(usdParEur = 1.1, xofParEur = 655.957, horodatage = 0L)

    @BeforeEach
    fun setUp() {
        every { transactionRepo.getByMonth(any(), any()) }  returns flowOf(emptyList())
        every { debtRepo.getAll() }                         returns flowOf(emptyList())
        every { savingsRepo.getAll() }                      returns flowOf(emptyList())
        every { categoryEnvelopeRepo.getAll() }              returns flowOf(emptyList())
        every { prefsRepo.get() }                            returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() }            returns flowOf(rates)
    }

    private fun income(amountCents: Long) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = Category.AUTRE, type = TransactionType.INCOME, date = LocalDate.of(2026, 4, 1)
    )

    private fun expense(amountCents: Long, category: Category, currency: Currency = Currency.EUR) = Transaction(
        amountCents = amountCents, currency = currency,
        category = category, type = TransactionType.EXPENSE, date = LocalDate.of(2026, 4, 1)
    )

    @Test
    fun `revenu moyen et taux d épargne sont nuls sans aucune transaction`() = runTest {
        val result = useCase(5, 2026).first()

        assertEquals(0L, result.revenuMoyenCents)
        assertNull(result.tauxEpargneActuelPct)
    }

    @Test
    fun `revenu moyen est la moyenne des revenus sur les 3 mois`() = runTest {
        every { transactionRepo.getByMonth(4, 2026) } returns flowOf(listOf(income(300_000L)))
        every { transactionRepo.getByMonth(3, 2026) } returns flowOf(listOf(income(200_000L)))
        every { transactionRepo.getByMonth(2, 2026) } returns flowOf(listOf(income(100_000L)))

        val result = useCase(5, 2026).first()

        assertEquals(200_000L, result.revenuMoyenCents)
    }

    @Test
    fun `un virement interne BRED vers TradeRepublic n'inflate pas le revenu moyen`() = runTest {
        val sortantBred = Transaction(
            amountCents = 50_000L, currency = Currency.EUR, category = Category.TRANSFERTS,
            type = TransactionType.EXPENSE, date = LocalDate.of(2026, 4, 1),
            importSource = "bred_csv", id = 1
        )
        val entrantTradeRepublic = Transaction(
            amountCents = 50_000L, currency = Currency.EUR, category = Category.TRANSFERTS,
            type = TransactionType.INCOME, date = LocalDate.of(2026, 4, 2),
            importSource = "trade_republic", id = 2
        )
        every { transactionRepo.getByMonth(4, 2026) } returns flowOf(
            listOf(income(200_000L), sortantBred, entrantTradeRepublic)
        )

        val result = useCase(5, 2026).first()

        // Le virement interne appairé ne doit compter ni en revenu ni en dépense
        assertEquals(200_000L / 3, result.revenuMoyenCents)
    }

    @Test
    fun `taux d épargne actuel reflète le ratio épargné sur le revenu`() = runTest {
        every { transactionRepo.getByMonth(4, 2026) } returns flowOf(
            listOf(income(200_000L), expense(50_000L, Category.ALIMENTATION))
        )
        every { transactionRepo.getByMonth(3, 2026) } returns flowOf(
            listOf(income(200_000L), expense(50_000L, Category.ALIMENTATION))
        )
        every { transactionRepo.getByMonth(2, 2026) } returns flowOf(
            listOf(income(200_000L), expense(50_000L, Category.ALIMENTATION))
        )

        val result = useCase(5, 2026).first()

        // (200 000 - 50 000) * 100 / 200 000 = 75%
        assertEquals(75, result.tauxEpargneActuelPct)
    }

    @Test
    fun `engagements incompressibles cumulent dettes et épargne`() = runTest {
        every { debtRepo.getAll() } returns flowOf(listOf(buildDebt(80_000L)))
        every { savingsRepo.getAll() } returns flowOf(listOf(buildSavings(20_000L)))

        val result = useCase(5, 2026).first()

        assertEquals(100_000L, result.engagementsMensuels)
    }

    @Test
    fun `poche recommandée exclut les catégories AUTRE et TRANSFERTS`() = runTest {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(expense(30_000L, Category.AUTRE), expense(30_000L, Category.TRANSFERTS))
        )

        val result = useCase(5, 2026).first()

        assertTrue(result.pouchesRecommandees.isEmpty())
    }

    @Test
    fun `poche recommandée arrondit exactement au 10€ supérieur quand le montant tombe juste`() = runTest {
        // 3 mois à 1 000 centimes chacun = moyenne exacte de 1 000 centimes (10€)
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(expense(1_000L, Category.ALIMENTATION))
        )

        val result = useCase(5, 2026).first()

        val poche = result.pouchesRecommandees.first { it.category == Category.ALIMENTATION }
        assertEquals(1_000L, poche.moyenneCents)
        assertEquals(1_000L, poche.recommandeCents)
        assertEquals(BudgetBucket.BESOINS, poche.bucket)
    }

    @Test
    fun `poche recommandée arrondit au 10€ supérieur quand le montant dépasse le cran`() = runTest {
        // 3 mois à 1 100 centimes chacun = moyenne de 1 100 centimes → arrondi à 2 000
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(expense(1_100L, Category.LOGEMENT))
        )

        val result = useCase(5, 2026).first()

        val poche = result.pouchesRecommandees.first { it.category == Category.LOGEMENT }
        assertEquals(2_000L, poche.recommandeCents)
    }

    @Test
    fun `estEquilibre est vrai quand le solde prévisionnel est positif`() = runTest {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(income(500_000L)))

        val result = useCase(5, 2026).first()

        assertTrue(result.estEquilibre)
        assertTrue(result.soldePrevisionelCents >= 0)
    }

    @Test
    fun `estEquilibre est faux quand les poches dépassent le revenu disponible`() = runTest {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(income(10_000L), expense(500_000L, Category.ALIMENTATION))
        )

        val result = useCase(5, 2026).first()

        assertFalse(result.estEquilibre)
        assertTrue(result.soldePrevisionelCents < 0)
    }

    @Test
    fun `objectif d épargne suit le taux cible des préférences`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(tauxEpargneCiblePct = 20))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(income(100_000L)))

        val result = useCase(5, 2026).first()

        assertEquals(20_000L, result.objectifEpargneCents)
        assertEquals(20, result.tauxEpargneCiblePct)
    }

    @Test
    fun `moisDeReference contient les 3 mois précédant le mois de référence`() = runTest {
        val result = useCase(5, 2026).first()

        assertEquals(listOf(4 to 2026, 3 to 2026, 2 to 2026), result.moisDeReference)
    }

    @Test
    fun `devise suit la devise par défaut des préférences`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(deviseParDefaut = Currency.XOF))

        val result = useCase(5, 2026).first()

        assertEquals(Currency.XOF, result.currency)
    }

    @Test
    fun `poche existante compare la recommandation à l enveloppe déjà configurée`() = runTest {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(expense(1_000L, Category.ALIMENTATION))
        )
        every { categoryEnvelopeRepo.getAll() } returns flowOf(
            listOf(CategoryEnvelope(category = Category.ALIMENTATION, plafondCents = 500L, currency = Currency.EUR))
        )

        val result = useCase(5, 2026).first()

        val poche = result.pouchesRecommandees.first { it.category == Category.ALIMENTATION }
        assertEquals(500L, poche.ecartAvecEnveloppe)
    }

    private fun buildDebt(monthlyPaymentCents: Long) = Debt(
        label = "Crédit test", totalCents = 10_000_000L,
        monthlyPaymentCents = monthlyPaymentCents, currency = Currency.EUR,
        type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.of(2026, 4, 1)
    )

    private fun buildSavings(monthlyContributionCents: Long) = SavingsAccount(
        id = 1L, type = SavingsType.LIVRET_A, label = "Livret A",
        currentBalanceCents = 0L, monthlyContributionCents = monthlyContributionCents,
        currency = Currency.EUR, updatedAt = LocalDate.of(2026, 4, 1)
    )
}
