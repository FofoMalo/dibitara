package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckAvailableFundsUseCaseTest {

    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase = mockk()
    private val useCase = CheckAvailableFundsUseCase(getMonthlyTransactions)

    private val today = LocalDate.of(2026, 5, 15)

    private fun buildTransaction(type: TransactionType, amountCents: Long) = Transaction(
        amountCents = amountCents,
        currency = Currency.EUR,
        category = Category.ALIMENTATION,
        type = type,
        date = today
    )

    @Test
    fun `retourne revenus moins dépenses du mois`() = runTest {
        val transactions = listOf(
            buildTransaction(TransactionType.INCOME,  300_000L),
            buildTransaction(TransactionType.EXPENSE, 120_000L),
            buildTransaction(TransactionType.EXPENSE,  30_000L)
        )
        every { getMonthlyTransactions(5, 2026) } returns flowOf(transactions)

        val solde = useCase(today)

        assertEquals(150_000L, solde) // 300 000 - 120 000 - 30 000
    }

    @Test
    fun `retourne 0 quand aucune transaction ce mois`() = runTest {
        every { getMonthlyTransactions(5, 2026) } returns flowOf(emptyList())

        val solde = useCase(today)

        assertEquals(0L, solde)
    }

    @Test
    fun `les investissements ne sont pas comptés dans le solde`() = runTest {
        val transactions = listOf(
            buildTransaction(TransactionType.INCOME,      200_000L),
            buildTransaction(TransactionType.INVESTMENT,  50_000L) // ne doit pas affecter le solde
        )
        every { getMonthlyTransactions(5, 2026) } returns flowOf(transactions)

        val solde = useCase(today)

        assertEquals(200_000L, solde)
    }

    @Test
    fun `solde négatif quand dépenses supérieures aux revenus`() = runTest {
        val transactions = listOf(
            buildTransaction(TransactionType.INCOME,   50_000L),
            buildTransaction(TransactionType.EXPENSE, 120_000L)
        )
        every { getMonthlyTransactions(5, 2026) } returns flowOf(transactions)

        val solde = useCase(today)

        assertEquals(-70_000L, solde)
    }
}
