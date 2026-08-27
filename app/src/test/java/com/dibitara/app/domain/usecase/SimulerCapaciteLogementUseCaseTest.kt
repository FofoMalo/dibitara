package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
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

class SimulerCapaciteLogementUseCaseTest {

    private val investmentRepo  : InvestmentRepository      = mockk()
    private val debtRepo        : DebtRepository            = mockk()
    private val savingsRepo     : SavingsRepository         = mockk()
    private val transactionRepo : TransactionRepository     = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()

    private val useCase = SimulerCapaciteLogementUseCase(
        getRealEstate               = GetRealEstateUseCase(investmentRepo),
        getDebts                    = GetDebtsUseCase(debtRepo),
        getSavings                  = GetSavingsUseCase(savingsRepo),
        getMonthlyTransactions      = GetMonthlyTransactionsUseCase(transactionRepo),
        userPreferencesRepo         = prefsRepo,
        exchangeRateRepo            = exchangeRateRepo,
        identifierVirementsInternes = IdentifierVirementsInternesUseCase()
    )

    private val rates = ExchangeRates(usdParEur = 1.1, xofParEur = 655.957, horodatage = 0L)
    private val maison = RealEstateAsset(
        id = 1L, label = "Maison", currentValueCents = 300_000_00L,
        currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1), debtId = 10L
    )

    @BeforeEach
    fun setUp() {
        every { investmentRepo.getAllRealEstate() } returns flowOf(listOf(maison))
        every { debtRepo.getAll() }                 returns flowOf(emptyList())
        every { savingsRepo.getAll() }               returns flowOf(emptyList())
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(emptyList())
        every { prefsRepo.get() }                    returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() }    returns flowOf(rates)
    }

    private fun income(amountCents: Long, date: LocalDate = LocalDate.of(2026, 7, 1)) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = Category.AUTRE, type = TransactionType.INCOME, date = date
    )

    private fun expense(amountCents: Long, category: Category, date: LocalDate = LocalDate.of(2026, 7, 1)) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = category, type = TransactionType.EXPENSE, date = date
    )

    // refMonth=8, refYear=2026 → 3 mois analysés : juillet, juin, mai 2026

    @Test
    fun `la mensualite du credit maison n'est pas comptee deux fois quand elle est aussi une depense LOGEMENT reelle`() = runTest {
        // Crédit maison : 1200 €/mois
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 10L, label = "Crédit maison", totalCents = 200_000_00L, monthlyPaymentCents = 1_200_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.of(2026, 8, 1))
        ))
        // La même mensualité, payée en pratique, est catégorisée LOGEMENT chaque mois (import bancaire réel)
        every { transactionRepo.getByMonth(7, 2026) } returns flowOf(listOf(expense(1_200_00L, Category.LOGEMENT)))
        every { transactionRepo.getByMonth(6, 2026) } returns flowOf(listOf(expense(1_200_00L, Category.LOGEMENT)))
        every { transactionRepo.getByMonth(5, 2026) } returns flowOf(listOf(expense(1_200_00L, Category.LOGEMENT)))

        val result = useCase(realEstateAssetId = 1L, revenuSimuleCents = 0L, refMonth = 8, refYear = 2026).first()

        assertNotNull(result)
        // La mensualité doit apparaître une seule fois dans le plancher : via les besoins réels (1200 €),
        // pas une deuxième fois ajoutée séparément.
        assertEquals(1_200_00L, result!!.besoinsIncompressiblesCents)
        assertEquals(1_200_00L, result.mensualiteMaisonCents)
        assertEquals(0L, result.autresEngagementsCents)
        assertEquals(1_200_00L, result.revenuPlancherCents)
    }

    @Test
    fun `aucun credit lie au bien laisse mensualiteMaisonCents a null sans affecter le plancher`() = runTest {
        val bienSansCredit = maison.copy(debtId = null)
        every { investmentRepo.getAllRealEstate() } returns flowOf(listOf(bienSansCredit))
        every { transactionRepo.getByMonth(7, 2026) } returns flowOf(listOf(expense(900_00L, Category.LOGEMENT)))

        val result = useCase(realEstateAssetId = 1L, revenuSimuleCents = 0L, refMonth = 8, refYear = 2026).first()

        assertNotNull(result)
        assertNull(result!!.mensualiteMaisonCents)
        assertEquals(300_00L, result.besoinsIncompressiblesCents) // 900 / 3 mois
    }

    @Test
    fun `revenu simule sous le plancher rend le scenario non tenable et suggere des reductions Envies`() = runTest {
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 10L, label = "Crédit maison", totalCents = 200_000_00L, monthlyPaymentCents = 1_200_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.of(2026, 8, 1))
        ))
        every { transactionRepo.getByMonth(7, 2026) } returns flowOf(listOf(
            expense(1_200_00L, Category.LOGEMENT),
            expense(600_00L, Category.LOISIRS)
        ))
        every { transactionRepo.getByMonth(6, 2026) } returns flowOf(listOf(
            expense(1_200_00L, Category.LOGEMENT),
            expense(600_00L, Category.LOISIRS)
        ))
        every { transactionRepo.getByMonth(5, 2026) } returns flowOf(listOf(
            expense(1_200_00L, Category.LOGEMENT),
            expense(600_00L, Category.LOISIRS)
        ))

        // Plancher = 1200 (besoins) = 1200 €. Revenu simulé = 1100 € < plancher + seuil (200 € par défaut).
        val result = useCase(realEstateAssetId = 1L, revenuSimuleCents = 1_100_00L, refMonth = 8, refYear = 2026).first()

        assertNotNull(result)
        assertFalse(result!!.estTenable)
        assertTrue(result.pochesEnviesReduction.isNotEmpty())
        // Coupe plafonnée à 50 % de la poche LOISIRS (600 €) = 300 € max
        assertTrue(result.pochesEnviesReduction.first().montantACouperCents <= 300_00L)
    }
}
