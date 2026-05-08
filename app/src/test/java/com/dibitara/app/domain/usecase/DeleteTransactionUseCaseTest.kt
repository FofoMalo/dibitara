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

class DeleteTransactionUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private lateinit var useCase: DeleteTransactionUseCase

    @BeforeEach
    fun setUp() { useCase = DeleteTransactionUseCase(repository) }

    @Test
    fun `délègue la suppression au repository`() = runTest {
        val transaction = Transaction(
            id = 1L, amountCents = 1000L, currency = Currency.EUR,
            category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
            date = LocalDate.now()
        )
        coJustRun { repository.delete(transaction) }

        val result = useCase(transaction)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.delete(transaction) }
    }
}
