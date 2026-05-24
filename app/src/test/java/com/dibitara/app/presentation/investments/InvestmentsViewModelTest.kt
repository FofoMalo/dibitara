package com.dibitara.app.presentation.investments

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.DeleteAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.DeleteCustomAssetUseCase
import com.dibitara.app.domain.usecase.DeleteEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.DeletePreciousMetalUseCase
import com.dibitara.app.domain.usecase.DeleteRealEstateUseCase
import com.dibitara.app.domain.usecase.DeleteScpiUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAirbnbRentalsByYearUseCase
import com.dibitara.app.domain.usecase.GetCustomAssetsUseCase
import com.dibitara.app.domain.usecase.GetEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.GetPreciousMetalsUseCase
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.GetScpiUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.SaveCustomAssetUseCase
import com.dibitara.app.domain.usecase.SaveEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.SavePreciousMetalUseCase
import com.dibitara.app.domain.usecase.SaveRealEstateUseCase
import com.dibitara.app.domain.usecase.SaveScpiUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.UpdateCustomAssetUseCase
import com.dibitara.app.domain.usecase.UpdateEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.UpdatePreciousMetalUseCase
import com.dibitara.app.domain.usecase.UpdateRealEstateUseCase
import com.dibitara.app.domain.usecase.UpdateScpiUseCase
import io.mockk.coEvery
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

    private val ucGetRealEstate: GetRealEstateUseCase = mockk()
    private val ucGetScpi: GetScpiUseCase = mockk()
    private val ucGetAirbnbByYear: GetAirbnbRentalsByYearUseCase = mockk()
    private val ucGetPreciousMetals: GetPreciousMetalsUseCase = mockk()
    private val ucGetCustomAssets: GetCustomAssetsUseCase = mockk()
    private val ucGetEmployeeSavings: GetEmployeeSavingsUseCase = mockk()
    private val ucSaveRealEstate: SaveRealEstateUseCase = mockk()
    private val ucSaveScpi: SaveScpiUseCase = mockk()
    private val ucSaveAirbnbRental: SaveAirbnbRentalUseCase = mockk()
    private val ucSavePreciousMetal: SavePreciousMetalUseCase = mockk(relaxed = true)
    private val ucSaveCustomAsset: SaveCustomAssetUseCase = mockk(relaxed = true)
    private val ucSaveEmployeeSavings: SaveEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucUpdateRealEstate: UpdateRealEstateUseCase = mockk()
    private val ucUpdateScpi: UpdateScpiUseCase = mockk()
    private val ucUpdateAirbnbRental: UpdateAirbnbRentalUseCase = mockk()
    private val ucUpdatePreciousMetal: UpdatePreciousMetalUseCase = mockk(relaxed = true)
    private val ucUpdateCustomAsset: UpdateCustomAssetUseCase = mockk(relaxed = true)
    private val ucUpdateEmployeeSavings: UpdateEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucDeleteRealEstate: DeleteRealEstateUseCase = mockk()
    private val ucDeleteScpi: DeleteScpiUseCase = mockk()
    private val ucDeleteAirbnbRental: DeleteAirbnbRentalUseCase = mockk()
    private val ucDeletePreciousMetal: DeletePreciousMetalUseCase = mockk(relaxed = true)
    private val ucDeleteCustomAsset: DeleteCustomAssetUseCase = mockk(relaxed = true)
    private val ucDeleteEmployeeSavings: DeleteEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucSaveVersement: SaveVersementUseCase = mockk()
    private val ucExisteVersementMois: ExisteVersementMoisUseCase = mockk()
    private val ucGetPreferences: GetUserPreferencesUseCase = mockk()
    private val ratesRepo: ExchangeRateRepository = mockk()

    private lateinit var viewModel: InvestmentsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { ucGetRealEstate() } returns flowOf(emptyList())
        every { ucGetScpi() } returns flowOf(emptyList())
        every { ucGetAirbnbByYear(any()) } returns flowOf(emptyList())
        every { ucGetPreciousMetals() } returns flowOf(emptyList())
        every { ucGetCustomAssets() } returns flowOf(emptyList())
        every { ucGetEmployeeSavings() } returns flowOf(emptyList())
        every { ucGetPreferences() } returns flowOf(UserPreferences())
        every { ratesRepo.getRatesFlow() } returns flowOf(ExchangeRates(1.09, 655.96, 0L))
        viewModel = InvestmentsViewModel(
            ucGetRealEstate, ucGetScpi, ucGetAirbnbByYear,
            ucGetPreciousMetals, ucGetCustomAssets, ucGetEmployeeSavings,
            ucSaveRealEstate, ucSaveScpi, ucSaveAirbnbRental,
            ucSavePreciousMetal, ucSaveCustomAsset, ucSaveEmployeeSavings,
            ucUpdateRealEstate, ucUpdateScpi, ucUpdateAirbnbRental,
            ucUpdatePreciousMetal, ucUpdateCustomAsset, ucUpdateEmployeeSavings,
            ucDeleteRealEstate, ucDeleteScpi, ucDeleteAirbnbRental,
            ucDeletePreciousMetal, ucDeleteCustomAsset, ucDeleteEmployeeSavings,
            ucSaveVersement, ucExisteVersementMois, ucGetPreferences, ratesRepo
        )
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
        coEvery { ucSaveRealEstate(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addRealEstate("Appartement Paris", "250000.00", Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addScpi avec parts valides émet Saved`() = runTest {
        coEvery { ucSaveScpi(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addScpi("SCPI Primovie", "10", "200.00", "50.00", Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `updateRealEstate avec valeur valide émet Saved`() = runTest {
        val asset = RealEstateAsset(id = 1L, label = "Appart", currentValueCents = 25000000L, currency = Currency.EUR, updatedAt = LocalDate.now())
        coEvery { ucUpdateRealEstate(any()) } returns Result.success(Unit)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.updateRealEstate(asset, "Appartement Lyon", "260000.00", Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addAirbnbRental avec montant valide émet Saved`() = runTest {
        coEvery { ucSaveAirbnbRental(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addAirbnbRental("Studio Bordeaux", "900.00", LocalDate.now(), Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }
}
