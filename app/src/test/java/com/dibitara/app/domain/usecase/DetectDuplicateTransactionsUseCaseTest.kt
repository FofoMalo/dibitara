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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetectDuplicateTransactionsUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private lateinit var useCase: DetectDuplicateTransactionsUseCase

    private val date = LocalDate.of(2025, 3, 15)

    @BeforeEach
    fun setUp() {
        useCase = DetectDuplicateTransactionsUseCase(repository)
    }

    @Test
    fun `liste vide — aucun groupe retourné`() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        val groupes = useCase().first()

        assertTrue(groupes.isEmpty())
    }

    @Test
    fun `deux transactions identiques — un groupe de deux retourné`() = runTest {
        val t1 = buildTx(id = 1L, date = date, amountCents = 4250L)
        val t2 = buildTx(id = 2L, date = date, amountCents = 4250L)
        every { repository.getAll() } returns flowOf(listOf(t1, t2))

        val groupes = useCase().first()

        assertEquals(1, groupes.size)
        assertEquals(2, groupes[0].transactions.size)
        // Par défaut on conserve la transaction avec le plus petit id
        assertEquals(1L, groupes[0].keepId)
    }

    @Test
    fun `trois transactions dont deux doublons — un seul groupe de deux`() = runTest {
        val doublon1 = buildTx(id = 1L, date = date, amountCents = 1000L)
        val doublon2 = buildTx(id = 2L, date = date, amountCents = 1000L)
        val unique   = buildTx(id = 3L, date = date, amountCents = 5000L)
        every { repository.getAll() } returns flowOf(listOf(doublon1, doublon2, unique))

        val groupes = useCase().first()

        assertEquals(1, groupes.size)
        assertEquals(2, groupes[0].transactions.size)
    }

    @Test
    fun `transactions différentes par montant — aucun doublon`() = runTest {
        val t1 = buildTx(id = 1L, date = date, amountCents = 1000L)
        val t2 = buildTx(id = 2L, date = date, amountCents = 2000L)
        every { repository.getAll() } returns flowOf(listOf(t1, t2))

        val groupes = useCase().first()

        assertTrue(groupes.isEmpty())
    }

    @Test
    fun `transactions mêmes montants mais types différents — aucun doublon`() = runTest {
        val depense = buildTx(id = 1L, date = date, amountCents = 1000L, type = TransactionType.EXPENSE)
        val revenu  = buildTx(id = 2L, date = date, amountCents = 1000L, type = TransactionType.INCOME)
        every { repository.getAll() } returns flowOf(listOf(depense, revenu))

        val groupes = useCase().first()

        assertTrue(groupes.isEmpty())
    }

    @Test
    fun `transactions mêmes montants mais dates différentes — aucun doublon`() = runTest {
        val t1 = buildTx(id = 1L, date = LocalDate.of(2025, 3, 15), amountCents = 1000L)
        val t2 = buildTx(id = 2L, date = LocalDate.of(2025, 3, 16), amountCents = 1000L)
        every { repository.getAll() } returns flowOf(listOf(t1, t2))

        val groupes = useCase().first()

        assertTrue(groupes.isEmpty())
    }

    @Test
    fun `deux groupes de doublons distincts — deux groupes retournés`() = runTest {
        val g1t1 = buildTx(id = 1L, date = date, amountCents = 500L)
        val g1t2 = buildTx(id = 2L, date = date, amountCents = 500L)
        val g2t1 = buildTx(id = 3L, date = LocalDate.of(2025, 4, 1), amountCents = 1500L)
        val g2t2 = buildTx(id = 4L, date = LocalDate.of(2025, 4, 1), amountCents = 1500L)
        every { repository.getAll() } returns flowOf(listOf(g1t1, g1t2, g2t1, g2t2))

        val groupes = useCase().first()

        assertEquals(2, groupes.size)
    }

    private fun buildTx(
        id: Long,
        date: LocalDate,
        amountCents: Long,
        type: TransactionType = TransactionType.EXPENSE
    ) = Transaction(
        id = id,
        amountCents = amountCents,
        currency = Currency.EUR,
        category = Category.ALIMENTATION,
        type = type,
        date = date
    )
}
