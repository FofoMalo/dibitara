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
 * Tests de GenerateRecurringUseCase - couvre les trois fréquences (MONTHLY, WEEKLY, YEARLY).
 */
class GenerateRecurringUseCaseTest {

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
    fun `MONTHLY - génère une occurrence quand le modèle est du mois dernier et l échéance est déjà passée`() = runTest {
        val today = LocalDate.of(2026, 8, 15)
        val template = makeTemplate(date = today.minusMonths(1), recurrenceDay = 5)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase(today)

        coVerify(exactly = 1) { repository.insert(match { it.sourceRecurringId == 1L && !it.isRecurring }) }
    }

    @Test
    fun `MONTHLY - ne génère pas l occurrence du mois en cours si son jour n est pas encore atteint`() = runTest {
        // Régression : la boucle comparait des mois (cursor vs until.withDayOfMonth(1)) au lieu de
        // dates réelles, ce qui matérialisait l'échéance du mois en cours avant son jour de prélèvement.
        val today = LocalDate.of(2026, 8, 15)
        val template = makeTemplate(date = today.minusMonths(1), recurrenceDay = 25)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false

        useCase(today)

        coVerify(exactly = 0) { repository.insert(any()) }
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
    fun `MONTHLY - sans recurrenceDay explicite, le jour n est pas plafonné au 28`() = runTest {
        // Bug corrigé : sans recurrenceDay, le jour venait de base.dayOfMonth plafonné à 28 -
        // un modèle créé le 30 générait ses occurrences le 28, en désaccord avec
        // GetCashflowProjectionUseCase et GetUpcomingPaymentsUseCase qui, eux, ne plafonnent pas.
        val today = LocalDate.of(2026, 5, 31)
        val template = makeTemplate(date = today.minusMonths(1).withDayOfMonth(30), recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase(today)

        coVerify { repository.insert(match { it.date == LocalDate.of(2026, 5, 30) }) }
    }

    @Test
    fun `MONTHLY - sans recurrenceDay explicite, le 31 est ramené au 28 en février`() = runTest {
        // Même correction que "prélèvement le 31 en février est ramené au 28" côté
        // GetCashflowProjectionUseCase (Correction #8) - ici via la longueur réelle du mois
        // cible (safeDay), pas via le plafond fixe à 28 qu'on vient de retirer.
        val template = makeTemplate(date = LocalDate.of(2026, 1, 31), recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(template))
        coEvery { repository.hasRecurringOccurrenceInRange(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase(LocalDate.of(2026, 2, 28))

        coVerify { repository.insert(match { it.date == LocalDate.of(2026, 2, 28) }) }
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
