package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.BankAccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeleteBankAccountUseCaseTest {

    private val repository: BankAccountRepository = mockk()
    private val useCase = DeleteBankAccountUseCase(repository)

    @Test
    fun `délègue la suppression au repository`() = runTest {
        val compte = BankAccount(
            id = 1, provider = BankProvider.BRED, label = "BRED", currentBalanceCents = 0L,
            currency = Currency.EUR, updatedAt = LocalDate.of(2026, 1, 1)
        )
        coEvery { repository.delete(compte) } returns Unit

        useCase(compte)

        coVerify { repository.delete(compte) }
    }
}
