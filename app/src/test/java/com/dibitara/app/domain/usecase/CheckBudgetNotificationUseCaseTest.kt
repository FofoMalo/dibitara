package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckBudgetNotificationUseCaseTest {

    private val getMonthlyBudget: GetMonthlyBudgetUseCase = mockk()
    private val useCase = CheckBudgetNotificationUseCase(getMonthlyBudget)

    private val today = LocalDate.of(2026, 5, 15)

    @Test
    fun `retourne le budget quand les dépenses dépassent le montant alloué`() = runTest {
        val budget = Budget(
            month = 5, year = 2026,
            allocatedCents = 100_000L, spentCents = 120_000L,
            currency = Currency.EUR
        )
        every { getMonthlyBudget(5, 2026) } returns flowOf(budget)

        val result = useCase(today)

        assertNotNull(result)
        assertEquals(budget, result)
    }

    @Test
    fun `retourne null quand le budget n est pas dépassé`() = runTest {
        val budget = Budget(
            month = 5, year = 2026,
            allocatedCents = 100_000L, spentCents = 80_000L,
            currency = Currency.EUR
        )
        every { getMonthlyBudget(5, 2026) } returns flowOf(budget)

        val result = useCase(today)

        assertNull(result)
    }

    @Test
    fun `retourne null quand aucun budget n est configuré pour le mois`() = runTest {
        every { getMonthlyBudget(5, 2026) } returns flowOf(null)

        val result = useCase(today)

        assertNull(result)
    }

    @Test
    fun `retourne null quand dépenses égales exactement au montant alloué`() = runTest {
        // isOverBudget = spentCents > allocatedCents (strictement supérieur)
        val budget = Budget(
            month = 5, year = 2026,
            allocatedCents = 100_000L, spentCents = 100_000L,
            currency = Currency.EUR
        )
        every { getMonthlyBudget(5, 2026) } returns flowOf(budget)

        val result = useCase(today)

        assertNull(result)
    }
}
