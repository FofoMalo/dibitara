package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetTransactionUseCasesTest {

    @Test
    fun `GetMonthlyTransactionsUseCase délègue au repository avec le bon mois`() {
        val repository: TransactionRepository = mockk()
        every { repository.getByMonth(5, 2026) } returns flowOf(emptyList())
        val useCase = GetMonthlyTransactionsUseCase(repository)

        val flow = useCase(5, 2026)

        assertNotNull(flow)
        verify(exactly = 1) { repository.getByMonth(5, 2026) }
    }

    @Test
    fun `GetMonthlyBudgetUseCase délègue au repository avec le bon mois`() {
        val repository: BudgetRepository = mockk()
        val budget = Budget(month = 5, year = 2026, allocatedCents = 200000L, spentCents = 0L, currency = Currency.EUR)
        every { repository.getBudget(5, 2026) } returns flowOf(budget)
        val useCase = GetMonthlyBudgetUseCase(repository)

        val flow = useCase(5, 2026)

        assertNotNull(flow)
        verify(exactly = 1) { repository.getBudget(5, 2026) }
    }
}
