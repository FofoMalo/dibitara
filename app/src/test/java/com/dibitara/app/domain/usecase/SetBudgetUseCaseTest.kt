package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.BudgetRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetBudgetUseCaseTest {

    private val repository: BudgetRepository = mockk(relaxed = true)
    private lateinit var useCase: SetBudgetUseCase

    @BeforeEach
    fun setUp() { useCase = SetBudgetUseCase(repository) }

    @Test
    fun `retourne échec si le montant est nul`() = runTest {
        val result = useCase(buildBudget(allocatedCents = 0L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `persiste le budget si le montant est valide`() = runTest {
        val budget = buildBudget(allocatedCents = 150000L)
        useCase(budget)
        coVerify(exactly = 1) { repository.upsert(budget) }
    }

    private fun buildBudget(allocatedCents: Long) = Budget(
        month = 5, year = 2026, allocatedCents = allocatedCents,
        spentCents = 0L, currency = Currency.EUR
    )
}
