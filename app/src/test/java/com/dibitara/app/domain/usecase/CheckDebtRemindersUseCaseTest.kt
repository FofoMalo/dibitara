package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckDebtRemindersUseCaseTest {

    private val getDebts: GetDebtsUseCase = mockk()
    private val useCase = CheckDebtRemindersUseCase(getDebts)

    // Aujourd'hui = 15 mai 2026
    private val today = LocalDate.of(2026, 5, 15)

    private fun buildDette(updatedAtDay: Int) = Debt(
        id = updatedAtDay.toLong(),
        label = "Dette $updatedAtDay",
        totalCents = 500_000L,
        monthlyPaymentCents = 10_000L,
        currency = Currency.EUR,
        type = DebtType.CREDIT_CONSO,
        updatedAt = LocalDate.of(2026, 4, updatedAtDay)
    )

    @Test
    fun `retourne les dettes dont le jour d échéance correspond à aujourd hui`() = runTest {
        val dettes = listOf(buildDette(15), buildDette(20))
        every { getDebts() } returns flowOf(dettes)

        val result = useCase(today)

        assertEquals(1, result.size)
        assertEquals("Dette 15", result.first().label)
    }

    @Test
    fun `retourne une liste vide quand aucune dette n a son échéance aujourd hui`() = runTest {
        val dettes = listOf(buildDette(1), buildDette(28))
        every { getDebts() } returns flowOf(dettes)

        val result = useCase(today)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `retourne plusieurs dettes si plusieurs échéances tombent le même jour`() = runTest {
        val dettes = listOf(buildDette(15), buildDette(15).copy(id = 99L, label = "Autre dette"))
        every { getDebts() } returns flowOf(dettes)

        val result = useCase(today)

        assertEquals(2, result.size)
    }

    @Test
    fun `retourne une liste vide quand aucune dette n existe`() = runTest {
        every { getDebts() } returns flowOf(emptyList())

        val result = useCase(today)

        assertTrue(result.isEmpty())
    }
}
