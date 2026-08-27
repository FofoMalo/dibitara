package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.VersementRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckPendingContributionsUseCaseTest {

    private val savingsRepo    : SavingsRepository    = mockk()
    private val investmentRepo : InvestmentRepository = mockk()
    private val versementRepo  : VersementRepository  = mockk()

    private val useCase = CheckPendingContributionsUseCase(savingsRepo, investmentRepo, versementRepo)

    private val today = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setUp() {
        every { savingsRepo.getAll() }        returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() } returns flowOf(emptyList())
    }

    private fun savings(id: Long, monthlyContributionCents: Long) = SavingsAccount(
        id = id, type = SavingsType.LIVRET_A, label = "Livret",
        currentBalanceCents = 0L, monthlyContributionCents = monthlyContributionCents,
        currency = Currency.EUR, updatedAt = today
    )

    private fun scpi(id: Long, monthlyContributionCents: Long) = ScpiInvestment(
        id = id, label = "SCPI", sharesCount = 1.0, shareValueCents = 100_000L,
        monthlyContributionCents = monthlyContributionCents, currency = Currency.EUR, updatedAt = today
    )

    @Test
    fun `aucune contribution en attente sans comptes ni SCPI`() = runTest {
        val result = useCase(5, 2026)

        assertEquals(0, result.count)
        assertEquals(0L, result.totalCents)
    }

    @Test
    fun `compte épargne avec contribution non versée est compté`() = runTest {
        every { savingsRepo.getAll() } returns flowOf(listOf(savings(1L, 20_000L)))
        coEvery { versementRepo.existsPourMois(1L, CompteType.EPARGNE, 2026, 5) } returns false

        val result = useCase(5, 2026)

        assertEquals(1, result.count)
        assertEquals(20_000L, result.totalCents)
    }

    @Test
    fun `compte épargne déjà versé n est pas compté`() = runTest {
        every { savingsRepo.getAll() } returns flowOf(listOf(savings(1L, 20_000L)))
        coEvery { versementRepo.existsPourMois(1L, CompteType.EPARGNE, 2026, 5) } returns true

        val result = useCase(5, 2026)

        assertEquals(0, result.count)
        assertEquals(0L, result.totalCents)
    }

    @Test
    fun `compte épargne sans contribution mensuelle n est pas compté`() = runTest {
        every { savingsRepo.getAll() } returns flowOf(listOf(savings(1L, 0L)))

        val result = useCase(5, 2026)

        assertEquals(0, result.count)
    }

    @Test
    fun `SCPI avec contribution non versée est comptée`() = runTest {
        every { investmentRepo.getAllScpi() } returns flowOf(listOf(scpi(10L, 30_000L)))
        coEvery { versementRepo.existsPourMois(10L, CompteType.SCPI, 2026, 5) } returns false

        val result = useCase(5, 2026)

        assertEquals(1, result.count)
        assertEquals(30_000L, result.totalCents)
    }

    @Test
    fun `épargne et SCPI en attente sont cumulées`() = runTest {
        every { savingsRepo.getAll() } returns flowOf(listOf(savings(1L, 20_000L)))
        every { investmentRepo.getAllScpi() } returns flowOf(listOf(scpi(10L, 30_000L)))
        coEvery { versementRepo.existsPourMois(1L, CompteType.EPARGNE, 2026, 5) } returns false
        coEvery { versementRepo.existsPourMois(10L, CompteType.SCPI, 2026, 5) } returns false

        val result = useCase(5, 2026)

        assertEquals(2, result.count)
        assertEquals(50_000L, result.totalCents)
    }
}
