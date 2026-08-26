package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RecurrenceFrequency
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

class GetUpcomingPaymentsUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = GetUpcomingPaymentsUseCase(repository)
    private val today = LocalDate.now()

    private fun template(
        recurrenceDay      : Int?                = 15,
        freq               : RecurrenceFrequency? = RecurrenceFrequency.MONTHLY,
        firstPaymentDate   : LocalDate?           = null,
        endDate            : LocalDate?           = null
    ) = Transaction(
        amountCents         = 10_000L,
        currency            = Currency.EUR,
        category            = Category.LOGEMENT,
        type                = TransactionType.EXPENSE,
        date                = today.minusMonths(1),
        isRecurring         = true,
        recurrenceDay       = recurrenceDay,
        recurrenceFrequency = freq,
        firstPaymentDate    = firstPaymentDate,
        endDate             = endDate
    )

    @Test
    fun `exclut les modèles dont la date de fin est dépassée`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(
            template(endDate = today.minusDays(1))
        ))

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `respecte la limite de résultats demandée`() = runTest {
        every { repository.getRecurring() } returns flowOf(
            (1..10).map { template(recurrenceDay = (it % 28) + 1) }
        )

        val result = useCase(limit = 3).first()

        assertEquals(3, result.size)
    }

    @Test
    fun `trie les échéances par date croissante`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(
            template(recurrenceDay = 5),
            template(recurrenceDay = 20)
        ))

        val result = useCase().first()

        assertTrue(result.zipWithNext().all { (a, b) -> a.nextDate <= b.nextDate })
    }

    @Test
    fun `échéance mensuelle est strictement après aujourd hui`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(template(recurrenceDay = 15)))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertTrue(result.first().nextDate > today)
    }

    @Test
    fun `échéance mensuelle le 31 n est pas plafonnée au 28`() = runTest {
        // Bug corrigé : le jour était systématiquement plafonné à 28, même dans un mois
        // de 30/31 jours (voir "Correction #8" déjà appliquée dans GetCashflowProjectionUseCase).
        every { repository.getRecurring() } returns flowOf(listOf(template(recurrenceDay = 31)))

        val result = useCase().first()

        val nextDate = result.first().nextDate
        val longueurMoisCible = nextDate.month.length(nextDate.isLeapYear)
        assertEquals(longueurMoisCible.coerceAtMost(31), nextDate.dayOfMonth)
    }

    @Test
    fun `échéance hebdomadaire retombe sur le même jour de semaine`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(
            template(
                recurrenceDay    = null,
                freq             = RecurrenceFrequency.WEEKLY,
                firstPaymentDate = today.minusWeeks(2)
            )
        ))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(today.minusWeeks(2).dayOfWeek, result.first().nextDate.dayOfWeek)
        assertTrue(result.first().nextDate > today)
    }

    @Test
    fun `échéance annuelle reprend le mois et le jour de la première date`() = runTest {
        val firstDate = today.minusYears(1)
        every { repository.getRecurring() } returns flowOf(listOf(
            template(recurrenceDay = null, freq = RecurrenceFrequency.YEARLY, firstPaymentDate = firstDate)
        ))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(firstDate.monthValue, result.first().nextDate.monthValue)
        assertTrue(result.first().nextDate > today)
    }

    @Test
    fun `fréquence nulle est traitée comme mensuelle`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(
            template(recurrenceDay = 15, freq = null)
        ))

        val result = useCase().first()

        assertEquals(1, result.size)
    }

    @Test
    fun `échéance calculée après la date de fin est exclue`() = runTest {
        // La prochaine échéance mensuelle tombera après-demain au plus tôt selon le jour,
        // mais on fixe une fin dès aujourd'hui pour forcer l'exclusion post-calcul
        every { repository.getRecurring() } returns flowOf(listOf(
            template(recurrenceDay = 15, endDate = today)
        ))

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `daysUntil reflète le nombre de jours jusqu à la prochaine échéance`() = runTest {
        every { repository.getRecurring() } returns flowOf(listOf(template(recurrenceDay = 15)))

        val result = useCase().first()

        assertEquals(
            java.time.temporal.ChronoUnit.DAYS.between(today, result.first().nextDate),
            result.first().daysUntil
        )
    }
}
