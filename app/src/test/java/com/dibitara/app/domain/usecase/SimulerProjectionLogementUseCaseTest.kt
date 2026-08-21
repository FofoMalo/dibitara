package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
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

class SimulerProjectionLogementUseCaseTest {

    private val debtRepo        : DebtRepository            = mockk()
    private val savingsRepo     : SavingsRepository         = mockk()
    private val transactionRepo : TransactionRepository     = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()

    private val useCase = SimulerProjectionLogementUseCase(
        getDebts             = GetDebtsUseCase(debtRepo),
        getSavings           = GetSavingsUseCase(savingsRepo),
        transactionRepository = transactionRepo,
        userPreferencesRepo  = prefsRepo,
        exchangeRateRepo     = exchangeRateRepo
    )

    private val rates = ExchangeRates(usdParEur = 1.1, xofParEur = 655.957, horodatage = 0L)
    private val today = LocalDate.of(2026, 8, 21)

    @BeforeEach
    fun setUp() {
        every { debtRepo.getAll() }               returns flowOf(emptyList())
        every { savingsRepo.getAll() }             returns flowOf(emptyList())
        every { transactionRepo.getRecurring() }   returns flowOf(emptyList())
        every { prefsRepo.get() }                  returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() }  returns flowOf(rates)
    }

    @Test
    fun `cumul constant chaque mois quand rien ne change dans la fenetre`() = runTest {
        val projection = useCase(
            maisonDebtId = null,
            revenuSimuleCents = 2_000_00L,
            besoinsIncompressiblesCents = 1_500_00L,
            horizonMois = 3,
            today = today
        ).first()

        assertEquals(3, projection.points.size)
        assertEquals(500_00L, projection.points[0].soldeCents)
        assertEquals(1_000_00L, projection.points[1].soldeCents)
        assertEquals(1_500_00L, projection.points[2].soldeCents)
        assertNull(projection.moisPassageNegatif)
    }

    @Test
    fun `une dette hors credit maison qui se termine dans la fenetre allege les mois suivants`() = runTest {
        // Dette de 300 € restant, mensualité 300 € → soldée après le 1er mois (nbMois = 300/300 = 1)
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 20L, label = "Crédit conso", totalCents = 300_00L, monthlyPaymentCents = 300_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_CONSO, updatedAt = today)
        ))

        val projection = useCase(
            maisonDebtId = 10L,
            revenuSimuleCents = 2_000_00L,
            besoinsIncompressiblesCents = 1_500_00L,
            horizonMois = 3,
            today = today
        ).first()

        // Mois 0 : engagement actif (300€) → net = 2000 - 1500 - 300 = 200
        assertEquals(200_00L, projection.points[0].soldeCents)
        // Mois 1 et 2 : dette soldée → net = 500 chacun, cumul 200 + 500 + 500 = 1200
        assertEquals(700_00L, projection.points[1].soldeCents)
        assertEquals(1_200_00L, projection.points[2].soldeCents)
    }

    @Test
    fun `une charge annuelle BESOINS a echeance dans la fenetre ponctionne le mois concerne`() = runTest {
        val echeanceDansLaFenetre = LocalDate.of(2026, 10, 5) // dans 2 mois
        every { transactionRepo.getRecurring() } returns flowOf(listOf(
            Transaction(
                id = 99L, amountCents = 600_00L, currency = Currency.EUR,
                category = Category.ASSURANCES, type = TransactionType.EXPENSE,
                date = echeanceDansLaFenetre, isRecurring = true,
                recurrenceFrequency = RecurrenceFrequency.YEARLY,
                firstPaymentDate = echeanceDansLaFenetre
            )
        ))

        val projection = useCase(
            maisonDebtId = null,
            revenuSimuleCents = 2_000_00L,
            besoinsIncompressiblesCents = 1_900_00L,
            horizonMois = 3,
            today = today
        ).first()

        // Mois 0 (août) et mois 1 (septembre) : pas de charge → net 100 chacun → cumul 100 puis 200
        assertEquals(100_00L, projection.points[0].soldeCents)
        assertEquals(200_00L, projection.points[1].soldeCents)
        // Mois 2 (octobre, contient le 5 octobre) : net = 100 - 600 = -500 → cumul 200 - 500 = -300
        assertEquals((-300_00L), projection.points[2].soldeCents)
        assertNotNull(projection.moisPassageNegatif)
    }
}
