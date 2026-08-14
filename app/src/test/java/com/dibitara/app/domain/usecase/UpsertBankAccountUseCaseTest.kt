package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.BankAccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UpsertBankAccountUseCaseTest {

    private val repository: BankAccountRepository = mockk()
    private val useCase = UpsertBankAccountUseCase(repository)

    private fun buildAccount(label: String = "BRED") = BankAccount(
        provider = BankProvider.BRED, label = label, currentBalanceCents = 0L,
        currency = Currency.EUR, updatedAt = LocalDate.of(2026, 1, 1)
    )

    @Test
    fun `insère un compte avec un libellé valide`() = runTest {
        coEvery { repository.upsert(any()) } returns 5L

        val result = useCase(buildAccount())

        assertTrue(result.isSuccess)
        assertEquals(5L, result.getOrThrow())
    }

    @Test
    fun `refuse un libellé vide sans appeler le repository`() = runTest {
        val result = useCase(buildAccount(label = "  "))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.upsert(any()) }
    }
}
