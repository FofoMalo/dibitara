package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EnregistrerMouvementCapitalUseCaseTest {

    private val repository: VersementRepository = mockk()
    private val useCase = EnregistrerMouvementCapitalUseCase(repository)
    private val date = LocalDate.of(2026, 8, 30)

    @Test
    fun `aucun mouvement ce mois - crée un nouveau versement`() = runTest {
        coEvery { repository.getPourMoisEtCompte(1L, CompteType.CUSTOM_ASSET, 2026, 8) } returns null
        coEvery { repository.save(any()) } returns Result.success(1L)

        val result = useCase(1L, CompteType.CUSTOM_ASSET, -1_000_00L, Currency.EUR, date)

        assertEquals(true, result.isSuccess)
        coVerify {
            repository.save(
                MonthlyVersement(accountId = 1L, compteType = CompteType.CUSTOM_ASSET, year = 2026, month = 8, montantCents = -1_000_00L, currency = Currency.EUR)
            )
        }
    }

    @Test
    fun `un mouvement déjà enregistré ce mois - le cumule au lieu de l'écraser`() = runTest {
        val existant = MonthlyVersement(id = 5L, accountId = 1L, compteType = CompteType.CUSTOM_ASSET, year = 2026, month = 8, montantCents = 500_00L, currency = Currency.EUR)
        coEvery { repository.getPourMoisEtCompte(1L, CompteType.CUSTOM_ASSET, 2026, 8) } returns existant
        coEvery { repository.update(any()) } returns Result.success(Unit)

        val result = useCase(1L, CompteType.CUSTOM_ASSET, -1_000_00L, Currency.EUR, date)

        assertEquals(true, result.isSuccess)
        coVerify { repository.update(existant.copy(montantCents = -500_00L)) }
    }
}
