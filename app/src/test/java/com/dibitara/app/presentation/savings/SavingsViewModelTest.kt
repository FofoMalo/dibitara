package com.dibitara.app.presentation.savings

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.DeleteChildUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsAccountUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveChildUseCase
import com.dibitara.app.domain.usecase.SaveSavingsAccountUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateSavingsAccountUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getSavings: GetSavingsUseCase = mockk()
    private val saveSavingsAccount: SaveSavingsAccountUseCase = mockk()
    private val updateSavingsAccount: UpdateSavingsAccountUseCase = mockk()
    private val deleteSavingsAccount: DeleteSavingsAccountUseCase = mockk()
    private val getChildren: GetChildrenUseCase = mockk()
    private val saveChild: SaveChildUseCase = mockk()
    private val deleteChild: DeleteChildUseCase = mockk()
    private val saveVersement: SaveVersementUseCase = mockk()
    private val existeVersementMois: ExisteVersementMoisUseCase = mockk()
    private val ucGetPreferences: GetUserPreferencesUseCase = mockk()
    private lateinit var viewModel: SavingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSavings() } returns flowOf(emptyList())
        every { getChildren() } returns flowOf(emptyList())
        every { ucGetPreferences() } returns flowOf(UserPreferences())
        viewModel = SavingsViewModel(
            getSavings, saveSavingsAccount, updateSavingsAccount,
            deleteSavingsAccount, getChildren, saveChild, deleteChild,
            saveVersement, existeVersementMois, ucGetPreferences
        )
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `saveAccount avec montant valide émet événement Saved`() = runTest {
        coEvery { saveSavingsAccount(any()) } returns Result.success(1L)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.saveAccount(SavingsType.LIVRET_A, "Livret A", "5000.00", "200.00", Currency.EUR, null)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addChild avec nom valide émet ChildSaved`() = runTest {
        coEvery { saveChild(any()) } returns Result.success(1L)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addChild("Emma")
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.ChildSaved })
        job.cancel()
    }

    @Test
    fun `updateAccount avec montant valide émet événement Saved`() = runTest {
        coEvery { updateSavingsAccount(any()) } returns Result.success(Unit)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.updateAccount(buildAccount(), SavingsType.LIVRET_A, "Livret A modifié", "6000.00", "300.00", Currency.EUR, null)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `deleteAccount appelle le usecase et émet Deleted`() = runTest {
        val compte = buildAccount()
        coEvery { deleteSavingsAccount(any()) } just Runs
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.deleteAccount(compte)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Deleted })
        job.cancel()
    }

    private fun buildAccount() = SavingsAccount(
        id = 1L,
        type = SavingsType.LIVRET_A,
        label = "Livret A",
        currentBalanceCents = 500000L,
        monthlyContributionCents = 20000L,
        currency = Currency.EUR,
        childId = null,
        updatedAt = LocalDate.now()
    )
}
