package com.dibitara.app.presentation.investments

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.usecase.DeleteInvestmentUseCase
import com.dibitara.app.domain.usecase.GetInvestmentsUseCase
import com.dibitara.app.domain.usecase.SaveInvestmentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class InvestmentsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getInvestments: GetInvestmentsUseCase = mockk()
    private val saveInvestment: SaveInvestmentUseCase = mockk()
    private val deleteInvestment: DeleteInvestmentUseCase = mockk()
    private lateinit var viewModel: InvestmentsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getInvestments.realEstate() } returns flowOf(emptyList())
        every { getInvestments.scpi() } returns flowOf(emptyList())
        every { getInvestments.airbnbByYear(any()) } returns flowOf(emptyList())
        viewModel = InvestmentsViewModel(getInvestments, saveInvestment, deleteInvestment)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial est Success avec listes vides`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is InvestmentsUiState.Success } as InvestmentsUiState.Success
        assertTrue(state.realEstate.isEmpty())
        assertTrue(state.scpi.isEmpty())
        assertTrue(state.airbnbRentals.isEmpty())
        job.cancel()
    }

    @Test
    fun `addRealEstate avec valeur valide émet Saved`() = runTest {
        coEvery { saveInvestment.realEstate(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addRealEstate("Appartement Paris", "250000.00", Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addScpi avec parts valides émet Saved`() = runTest {
        coEvery { saveInvestment.scpi(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addScpi("SCPI Primovie", "10", "200.00", "50.00", Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addAirbnbRental avec montant valide émet Saved`() = runTest {
        coEvery { saveInvestment.airbnb(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addAirbnbRental("Studio Bordeaux", "900.00", LocalDate.now(), Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }
}
