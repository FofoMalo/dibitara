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

class GetTransactionSuggestionsUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val useCase = GetTransactionSuggestionsUseCase(repository)
    private val aujourd = LocalDate.now()

    @Test
    fun `retourne liste vide si aucune transaction`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(emptyList())

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `exclut les transactions avec une seule occurrence`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "Café", amountCents = 350L))
        )

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `crée une suggestion pour 2 occurrences identiques`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(note = "Café", amountCents = 350L),
                buildTransaction(note = "Café", amountCents = 350L)
            )
        )

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("Café", result.first().label)
        assertEquals(350L, result.first().amountCents)
        assertEquals(2, result.first().frequence)
    }

    @Test
    fun `trie par fréquence décroissante`() = runTest {
        // "Loyer" apparaît 3 fois, "Café" 2 fois → Loyer doit être en premier
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(note = "Café", amountCents = 350L),
                buildTransaction(note = "Café", amountCents = 350L),
                buildTransaction(note = "Loyer", amountCents = 80000L),
                buildTransaction(note = "Loyer", amountCents = 80000L),
                buildTransaction(note = "Loyer", amountCents = 80000L)
            )
        )

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("Loyer", result[0].label)
        assertEquals("Café", result[1].label)
    }

    @Test
    fun `limite à 5 suggestions maximum`() = runTest {
        // 6 groupes distincts, chacun avec 2 occurrences
        val transactions = (1..6).flatMap { i ->
            listOf(
                buildTransaction(note = "Transaction $i", amountCents = i * 100L),
                buildTransaction(note = "Transaction $i", amountCents = i * 100L)
            )
        }
        every { repository.getByDateRange(any(), any()) } returns flowOf(transactions)

        val result = useCase().first()

        assertEquals(5, result.size)
    }

    @Test
    fun `exclut les transactions récurrentes`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(note = "Loyer", amountCents = 80000L, isRecurring = true),
                buildTransaction(note = "Loyer", amountCents = 80000L, isRecurring = true)
            )
        )

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `exclut les transactions sans libellé`() = runTest {
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(note = "", amountCents = 500L),
                buildTransaction(note = "", amountCents = 500L)
            )
        )

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `ignore la casse pour le regroupement`() = runTest {
        // "café" et "Café" doivent être regroupés en une seule suggestion
        every { repository.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(note = "café", amountCents = 350L),
                buildTransaction(note = "Café", amountCents = 350L)
            )
        )

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(2, result.first().frequence)
    }

    private fun buildTransaction(
        note: String,
        amountCents: Long,
        isRecurring: Boolean = false
    ) = Transaction(
        amountCents = amountCents,
        currency    = Currency.EUR,
        category    = Category.ALIMENTATION,
        type        = TransactionType.EXPENSE,
        date        = aujourd,
        note        = note,
        isRecurring = isRecurring
    )
}
