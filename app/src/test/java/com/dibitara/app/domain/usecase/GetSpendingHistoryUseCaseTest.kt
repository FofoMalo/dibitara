package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetSpendingHistoryUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val useCase = GetSpendingHistoryUseCase(repository)

    @Test
    fun `retourne 6 entrées même si aucune dépense`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(6, result.size)
        assertTrue(result.all { it.totalCents == 0L })
    }

    @Test
    fun `comptabilise seulement les transactions EXPENSE`() = runTest {
        val aujourd = LocalDate.now()
        val transactions = listOf(
            Transaction(amountCents = 5000L, currency = Currency.EUR,
                category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
                date = aujourd),
            Transaction(amountCents = 10000L, currency = Currency.EUR,
                category = Category.AUTRE, type = TransactionType.INCOME,
                date = aujourd)
        )
        every { repository.getByDateRange(any(), any()) } returns flowOf(transactions)

        val result = useCase().first()

        // Le mois courant est le dernier élément de la liste
        val moisCourant = result.last()
        assertEquals(5000L, moisCourant.totalCents)
    }

    @Test
    fun `regroupe correctement par mois`() = runTest {
        val aujourd = LocalDate.now()
        val moisPrecedent = aujourd.minusMonths(1)
        val transactions = listOf(
            Transaction(amountCents = 3000L, currency = Currency.EUR,
                category = Category.TRANSPORT, type = TransactionType.EXPENSE,
                date = aujourd),
            Transaction(amountCents = 7000L, currency = Currency.EUR,
                category = Category.LOGEMENT, type = TransactionType.EXPENSE,
                date = moisPrecedent)
        )
        every { repository.getByDateRange(any(), any()) } returns flowOf(transactions)

        val result = useCase().first()

        assertEquals(6, result.size)
        assertEquals(3000L, result.last().totalCents)
        assertEquals(7000L, result[result.size - 2].totalCents)
    }
}
