package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UpdateTransactionUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private lateinit var useCase: UpdateTransactionUseCase

    @BeforeEach
    fun setUp() { useCase = UpdateTransactionUseCase(repository) }

    @Test
    fun `retourne succès et appelle repository quand le montant est valide`() = runTest {
        val transaction = buildTransaction(amountCents = 500L)
        coJustRun { repository.update(transaction) }

        val result = useCase(transaction)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.update(transaction) }
    }

    @Test
    fun `retourne échec sans appeler le repository quand le montant est nul`() = runTest {
        val transaction = buildTransaction(amountCents = 0L)

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Le montant doit être supérieur à 0", result.exceptionOrNull()?.message)
    }

    private fun buildTransaction(amountCents: Long) = Transaction(
        id = 1L, amountCents = amountCents, currency = Currency.EUR,
        category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
        date = LocalDate.now()
    )
}
