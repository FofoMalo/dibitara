package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserPreferencesUseCasesTest {

    private val repository: UserPreferencesRepository = mockk(relaxed = true)

    // ─── GetUserPreferencesUseCase ────────────────────────────────────────────

    @Test
    fun `GetUserPreferences retourne les préférences depuis le repository`() = runTest {
        val prefs = UserPreferences(seuilFondsCents = 75_000L, deviseParDefaut = Currency.USD)
        every { repository.get() } returns flowOf(prefs)

        val result = GetUserPreferencesUseCase(repository)().first()

        assertEquals(75_000L, result.seuilFondsCents)
        assertEquals(Currency.USD, result.deviseParDefaut)
    }

    @Test
    fun `GetUserPreferences retourne les valeurs par défaut si le repository émet les défauts`() = runTest {
        every { repository.get() } returns flowOf(UserPreferences())

        val result = GetUserPreferencesUseCase(repository)().first()

        assertEquals(50_000L, result.seuilFondsCents)
        assertEquals(Currency.EUR, result.deviseParDefaut)
    }

    // ─── UpdateSeuilFondsUseCase ──────────────────────────────────────────────

    @Test
    fun `UpdateSeuilFonds délègue la mise à jour au repository`() = runTest {
        UpdateSeuilFondsUseCase(repository)(120_000L)

        coVerify { repository.updateSeuil(120_000L) }
    }

    // ─── UpdateDeviseParDefautUseCase ─────────────────────────────────────────

    @Test
    fun `UpdateDeviseParDefaut délègue la mise à jour au repository`() = runTest {
        UpdateDeviseParDefautUseCase(repository)(Currency.XOF)

        coVerify { repository.updateDevise(Currency.XOF) }
    }
}
