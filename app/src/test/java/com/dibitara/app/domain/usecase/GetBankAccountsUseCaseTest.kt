package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.BankAccountRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetBankAccountsUseCaseTest {

    private val repository: BankAccountRepository = mockk()
    private val useCase = GetBankAccountsUseCase(repository)

    @Test
    fun `délègue au repository`() = runTest {
        val comptes = listOf(
            BankAccount(id = 1, provider = BankProvider.BRED, label = "BRED", currentBalanceCents = 10_000L, currency = Currency.EUR, updatedAt = LocalDate.of(2026, 1, 1))
        )
        every { repository.getAll() } returns flowOf(comptes)

        assertEquals(comptes, useCase().first())
    }
}
