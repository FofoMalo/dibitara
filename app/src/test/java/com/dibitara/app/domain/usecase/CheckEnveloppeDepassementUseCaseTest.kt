package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckEnveloppeDepassementUseCaseTest {

    private val enveloppeRepo   : CategoryEnvelopeRepository = mockk()
    private val transactionRepo : TransactionRepository      = mockk()

    private val useCase = CheckEnveloppeDepassementUseCase(enveloppeRepo, transactionRepo)

    private val today = LocalDate.now()

    private fun envelope(category: Category, plafondCents: Long) =
        CategoryEnvelope(category = category, plafondCents = plafondCents, currency = Currency.EUR)

    private fun expense(category: Category, amountCents: Long) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = category, type = TransactionType.EXPENSE, date = today
    )

    @Test
    fun `retourne une liste vide sans aucune enveloppe configurée`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(emptyList())

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `enveloppe sous le seuil n est pas remontée`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(listOf(envelope(Category.ALIMENTATION, 10_000L)))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            expense(Category.ALIMENTATION, 1_000L)  // 10% du plafond
        ))

        val result = useCase(seuilTaux = 0.8f)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `enveloppe au-delà du seuil est remontée avec le bon taux`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(listOf(envelope(Category.ALIMENTATION, 10_000L)))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            expense(Category.ALIMENTATION, 9_000L)  // 90% du plafond
        ))

        val result = useCase(seuilTaux = 0.8f)

        assertEquals(1, result.size)
        assertEquals(9_000L, result.first().depenseCents)
        assertEquals(0.9f, result.first().taux)
    }

    @Test
    fun `enveloppe sans plafond configuré n est jamais dépassée`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(listOf(envelope(Category.ALIMENTATION, 0L)))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            expense(Category.ALIMENTATION, 5_000L)
        ))

        val result = useCase(seuilTaux = 0.01f)

        // plafondCents <= 0 → taux forcé à 0f, donc jamais >= un seuil strictement positif
        assertTrue(result.isEmpty())
    }

    @Test
    fun `catégorie sans dépense ce mois a un taux nul`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(listOf(envelope(Category.LOISIRS, 10_000L)))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(emptyList())

        val result = useCase(seuilTaux = 0.0f)

        assertEquals(1, result.size)
        assertEquals(0L, result.first().depenseCents)
        assertEquals(0f, result.first().taux)
    }

    @Test
    fun `seuil personnalisé filtre correctement les enveloppes`() = runTest {
        every { enveloppeRepo.getAll() } returns flowOf(listOf(envelope(Category.ALIMENTATION, 10_000L)))
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            expense(Category.ALIMENTATION, 10_000L)  // 100% = plafond atteint
        ))

        val result = useCase(seuilTaux = 1.0f)

        assertEquals(1, result.size)
        assertTrue(result.first().isDepasse)
    }
}
