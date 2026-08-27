package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SommeVersementsDepuisUseCaseTest {

    private val repository: VersementRepository = mockk()
    private val useCase = SommeVersementsDepuisUseCase(repository)

    private fun versement(year: Int, month: Int, montant: Long) = MonthlyVersement(
        accountId = 1L, compteType = CompteType.SCPI, year = year, month = month,
        montantCents = montant, currency = Currency.EUR
    )

    @Test
    fun `ne somme que les versements à partir du mois d'acquisition inclus`() = runTest {
        every { repository.getForAccount(1L, CompteType.SCPI) } returns flowOf(listOf(
            versement(2024, 12, 100_00L), // avant acquisition - exclu
            versement(2025, 3, 100_00L),  // mois d'acquisition - inclus
            versement(2025, 4, 100_00L)   // après acquisition - inclus
        ))

        val total = useCase(1L, CompteType.SCPI, LocalDate.of(2025, 3, 1))

        assertEquals(200_00L, total)
    }

    @Test
    fun `aucun versement - retourne zéro`() = runTest {
        every { repository.getForAccount(1L, CompteType.SCPI) } returns flowOf(emptyList())

        val total = useCase(1L, CompteType.SCPI, LocalDate.of(2025, 3, 1))

        assertEquals(0L, total)
    }
}
