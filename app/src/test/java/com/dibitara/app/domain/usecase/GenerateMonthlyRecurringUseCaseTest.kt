package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests de GenerateRecurringUseCase — couvre les trois fréquences (MONTHLY, WEEKLY, YEARLY).
 */
class GenerateMonthlyRecurringUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val useCase = GenerateRecurringUseCase(repository)

    private fun makeTemplate(
        id: Long = 1L,
        date: LocalDate = LocalDate.now().minusMonths(1),
        freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        recurrenceDay: Int? = 5,
        endDate: LocalDate? = null
    ) = Transaction(
        id = id,
        amountCents = 80000L,
        currency = Currency.EUR,
        category = Category.LOGEMENT,
        type = TransactionType.EXPENSE,
        date = date,
        note = "Loyer",
        isRecurring = true,
        recurrenceDay = recurrenceDay,
        recurrenceFrequency = freq,
        firstPaymentDate = date,
        endDate = endDate
    )

    // ─── MENSUEL ──────────────────────────────────────────────────────────────

    @Test
    fun `MONTHLY - génère une occurrence quand le modèle est du mois dernier et aucune occurrence ce mois`() = runTest {
        val template = makeTemplate()
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase()

        coVerify(exactly = 1) { repository.insert(match { it.sourceRecurringId == 1L && !it.isRecurring }) }
    }

    @Test
    fun `MONTHLY - ne génère pas si une occurrence existe déjà ce mois`() = runTest {
        val template = makeTemplate()
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns true

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `MONTHLY - ne génère pas si le modèle a été créé ce mois-ci`() = runTest {
        val template = makeTemplate(date = LocalDate.now())
        every { repository.getRecurring() } returns flowOf(listOf(template))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `MONTHLY - l occurrence a la date correcte avec le bon jour du mois`() = runTest {
        val template = makeTemplate()
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase()

        val today = LocalDate.now()
        coVerify {
            repository.insert(match { t ->
                t.date.monthValue == today.monthValue
                        && t.date.year == today.year
                        && t.date.dayOfMonth == 5
            })
        }
    }

    @Test
    fun `MONTHLY - ne génère pas si on a dépassé endDate`() = runTest {
        val template = makeTemplate(endDate = LocalDate.now().minusDays(1))
        every { repository.getRecurring() } returns flowOf(listOf(template))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    // ─── HEBDOMADAIRE ─────────────────────────────────────────────────────────

    @Test
    fun `WEEKLY - génère une occurrence la semaine suivante si aucune n existe`() = runTest {
        val base = LocalDate.now().minusWeeks(1)
        val template = makeTemplate(date = base, freq = RecurrenceFrequency.WEEKLY, recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase()

        coVerify(atLeast = 1) { repository.insert(match { !it.isRecurring && it.sourceRecurringId == 1L }) }
    }

    @Test
    fun `WEEKLY - ne génère pas si modèle créé cette semaine`() = runTest {
        val template = makeTemplate(date = LocalDate.now(), freq = RecurrenceFrequency.WEEKLY, recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    // ─── ANNUEL ───────────────────────────────────────────────────────────────

    @Test
    fun `YEARLY - génère une occurrence l année suivante si aucune n existe`() = runTest {
        val base = LocalDate.now().minusYears(1)
        val template = makeTemplate(date = base, freq = RecurrenceFrequency.YEARLY, recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase()

        coVerify(exactly = 1) { repository.insert(match { !it.isRecurring && it.sourceRecurringId == 1L }) }
    }

    @Test
    fun `YEARLY - ne génère pas si modèle créé cette année`() = runTest {
        val template = makeTemplate(date = LocalDate.now(), freq = RecurrenceFrequency.YEARLY, recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }
}
