package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GenerateMonthlyRecurringUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val useCase = GenerateMonthlyRecurringUseCase(repository)

    // Modèle récurrent créé le mois dernier (doit déclencher une génération ce mois-ci)
    private val templateLastMonth = Transaction(
        id = 1L,
        amountCents = 80000L,
        currency = Currency.EUR,
        category = Category.LOGEMENT,
        type = TransactionType.EXPENSE,
        date = LocalDate.now().minusMonths(1),
        note = "Loyer",
        isRecurring = true,
        recurrenceDay = 5
    )

    // Modèle récurrent créé CE mois-ci (ne doit PAS déclencher de génération)
    private val templateThisMonth = Transaction(
        id = 2L,
        amountCents = 1500L,
        currency = Currency.EUR,
        category = Category.LOISIRS,
        type = TransactionType.EXPENSE,
        date = LocalDate.now(),
        note = "Abonnement streaming",
        isRecurring = true,
        recurrenceDay = 1
    )

    @Test
    fun `génère une occurrence quand le modèle est du mois dernier et aucune occurrence ce mois`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(templateLastMonth))
        coEvery { repository.hasRecurringOccurrenceThisMonth(1L, any(), any()) } returns false
        coEvery { repository.insert(any()) } returns 10L

        useCase()

        coVerify(exactly = 1) { repository.insert(match { it.sourceRecurringId == 1L && !it.isRecurring }) }
    }

    @Test
    fun `ne génère pas si une occurrence existe déjà ce mois`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(templateLastMonth))
        coEvery { repository.hasRecurringOccurrenceThisMonth(1L, any(), any()) } returns true

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `ne génère pas si le modèle a été créé ce mois-ci`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(templateThisMonth))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `ne génère pas si le modèle n a pas de jour de récurrence`() = runTest {
        val templateSansJour = templateLastMonth.copy(recurrenceDay = null)
        every { repository.getRecurring() } returns flowOf(listOf(templateSansJour))

        useCase()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `l occurrence générée a la date correcte avec le bon jour du mois`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(templateLastMonth))
        coEvery { repository.hasRecurringOccurrenceThisMonth(1L, any(), any()) } returns false
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
}
