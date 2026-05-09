package com.dibitara.app.presentation.settings

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.UpdateSeuilFondsUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val ucGet: GetUserPreferencesUseCase = mockk()
    private val ucSeuil: UpdateSeuilFondsUseCase = mockk(relaxed = true)
    private val ucDevise: UpdateDeviseParDefautUseCase = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { ucGet() } returns flowOf(UserPreferences())
        viewModel = SettingsViewModel(ucGet, ucSeuil, ucDevise)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `préférences exposent les valeurs par défaut au démarrage`() = runTest {
        val prefs = viewModel.preferences.value

        assertEquals(50_000L, prefs.seuilFondsCents)
        assertEquals(Currency.EUR, prefs.deviseParDefaut)
    }

    @Test
    fun `mettreAJourSeuil convertit les euros en centimes avant d appeler le UseCase`() = runTest {
        viewModel.mettreAJourSeuil("300")
        testScheduler.advanceUntilIdle()

        coVerify { ucSeuil(30_000L) } // 300€ × 100 = 30 000 centimes
    }

    @Test
    fun `mettreAJourSeuil ignore une saisie non numérique`() = runTest {
        viewModel.mettreAJourSeuil("abc")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { ucSeuil(any()) }
    }

    @Test
    fun `mettreAJourDevise délègue au UseCase`() = runTest {
        viewModel.mettreAJourDevise(Currency.USD)
        testScheduler.advanceUntilIdle()

        coVerify { ucDevise(Currency.USD) }
    }
}
