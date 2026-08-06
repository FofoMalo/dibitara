package com.dibitara.app.presentation.investments

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.CalculerTendancePatrimoineUseCase
import com.dibitara.app.domain.usecase.DeleteAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.DeleteCustomAssetUseCase
import com.dibitara.app.domain.usecase.DeleteEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.DeleteRealEstateUseCase
import com.dibitara.app.domain.usecase.DeleteScpiUseCase
import com.dibitara.app.domain.usecase.DeleteVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAirbnbRentalsByYearUseCase
import com.dibitara.app.domain.usecase.GetCustomAssetsUseCase
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.GetEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.GetScpiUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.GetVehicleRentalEntriesUseCase
import com.dibitara.app.domain.usecase.SaveAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.SaveCustomAssetUseCase
import com.dibitara.app.domain.usecase.SaveEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.SaveRealEstateUseCase
import com.dibitara.app.domain.usecase.SaveScpiUseCase
import com.dibitara.app.domain.usecase.SaveVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.UpdateCustomAssetUseCase
import com.dibitara.app.domain.usecase.UpdateEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.UpdateRealEstateUseCase
import com.dibitara.app.domain.usecase.UpdateScpiUseCase
import com.dibitara.app.domain.usecase.UpdateVehicleRentalEntryUseCase
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
    private val ucGetVehicleRentals: GetVehicleRentalEntriesUseCase = mockk()
    private val ucGetCustomAssets: GetCustomAssetsUseCase = mockk()
    private val ucGetEmployeeSavings: GetEmployeeSavingsUseCase = mockk()
    private val ucSaveRealEstate: SaveRealEstateUseCase = mockk()
    private val ucSaveScpi: SaveScpiUseCase = mockk()
    private val ucSaveAirbnbRental: SaveAirbnbRentalUseCase = mockk()
    private val ucSaveVehicleEntry: SaveVehicleRentalEntryUseCase = mockk()
    private val ucSaveCustomAsset: SaveCustomAssetUseCase = mockk(relaxed = true)
    private val ucSaveEmployeeSavings: SaveEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucUpdateRealEstate: UpdateRealEstateUseCase = mockk()
    private val ucUpdateScpi: UpdateScpiUseCase = mockk()
    private val ucUpdateAirbnbRental: UpdateAirbnbRentalUseCase = mockk()
    private val ucUpdateVehicleEntry: UpdateVehicleRentalEntryUseCase = mockk()
    private val ucUpdateCustomAsset: UpdateCustomAssetUseCase = mockk(relaxed = true)
    private val ucUpdateEmployeeSavings: UpdateEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucDeleteRealEstate: DeleteRealEstateUseCase = mockk()
    private val ucDeleteScpi: DeleteScpiUseCase = mockk()
    private val ucDeleteAirbnbRental: DeleteAirbnbRentalUseCase = mockk()
    private val ucDeleteVehicleEntry: DeleteVehicleRentalEntryUseCase = mockk(relaxed = true)
    private val ucDeleteCustomAsset: DeleteCustomAssetUseCase = mockk(relaxed = true)
    private val ucDeleteEmployeeSavings: DeleteEmployeeSavingsUseCase = mockk(relaxed = true)
    private val ucSaveVersement: SaveVersementUseCase = mockk()
    private val ucExisteVersementMois: ExisteVersementMoisUseCase = mockk()
    private val ucGetPreferences: GetUserPreferencesUseCase = mockk()
    private val ratesRepo: ExchangeRateRepository = mockk()
    private val ucGetDebts: GetDebtsUseCase = mockk()
    private val ucGetPatrimoineHistory: GetPatrimoineHistoryUseCase = mockk()
    private val ucCalculerTendance: CalculerTendancePatrimoineUseCase = mockk()

    private lateinit var viewModel: InvestmentsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { ucGetRealEstate() } returns flowOf(emptyList())
        every { ucGetScpi() } returns flowOf(emptyList())
        every { ucGetAirbnbByYear(any()) } returns flowOf(emptyList())
        every { ucGetVehicleRentals() } returns flowOf(emptyList())
        every { ucGetCustomAssets() } returns flowOf(emptyList())
        every { ucGetEmployeeSavings() } returns flowOf(emptyList())
        every { ucGetPreferences() } returns flowOf(UserPreferences())
        every { ratesRepo.getRatesFlow() } returns flowOf(ExchangeRates(1.09, 655.96, 0L))
        every { ucGetDebts() } returns flowOf(emptyList())
        every { ucGetPatrimoineHistory() } returns flowOf(emptyList())
        every { ucCalculerTendance(any()) } returns null
        viewModel = InvestmentsViewModel(
            ucGetRealEstate, ucGetScpi, ucGetAirbnbByYear, ucGetVehicleRentals,
            ucGetCustomAssets, ucGetEmployeeSavings,
            ucSaveRealEstate, ucSaveScpi, ucSaveAirbnbRental, ucSaveVehicleEntry,
            ucSaveCustomAsset, ucSaveEmployeeSavings,
            ucUpdateRealEstate, ucUpdateScpi, ucUpdateAirbnbRental, ucUpdateVehicleEntry,
            ucUpdateCustomAsset, ucUpdateEmployeeSavings,
            ucDeleteRealEstate, ucDeleteScpi, ucDeleteAirbnbRental, ucDeleteVehicleEntry,
            ucDeleteCustomAsset, ucDeleteEmployeeSavings,
            ucSaveVersement, ucExisteVersementMois, ucGetPreferences, ratesRepo, ucGetDebts,
            ucGetPatrimoineHistory, ucCalculerTendance
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
        assertTrue(state.vehicleRentalEntries.isEmpty())
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

    @Test
    fun `addVehicleRentalEntry avec montant valide émet Saved`() = runTest {
        coEvery { ucSaveVehicleEntry(any()) } returns Result.success(1L)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addVehicleRentalEntry("Location weekend", "150.00", VehicleEntryType.REVENU, LocalDate.now(), Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `updateVehicleRentalEntry avec montant valide émet Saved`() = runTest {
        val entry = com.dibitara.app.domain.model.VehicleRentalEntry(
            id = 1L, label = "Vidange", entryType = VehicleEntryType.CHARGE,
            amountCents = 8000L, date = LocalDate.now(), currency = Currency.EUR
        )
        coEvery { ucUpdateVehicleEntry(any()) } returns Result.success(Unit)
        val events = mutableListOf<InvestmentsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.updateVehicleRentalEntry(entry, "Vidange + filtre", "95.00", VehicleEntryType.CHARGE, LocalDate.now(), Currency.EUR)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is InvestmentsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `uiState sépare les revenus et les charges du véhicule locatif`() = runTest {
        val revenu = com.dibitara.app.domain.model.VehicleRentalEntry(
            id = 1L, label = "Location weekend", entryType = VehicleEntryType.REVENU,
            amountCents = 15000L, date = LocalDate.now(), currency = Currency.EUR
        )
        val charge = com.dibitara.app.domain.model.VehicleRentalEntry(
            id = 2L, label = "Vidange", entryType = VehicleEntryType.CHARGE,
            amountCents = 8000L, date = LocalDate.now(), currency = Currency.EUR
        )
        every { ucGetVehicleRentals() } returns flowOf(listOf(revenu, charge))

        viewModel = InvestmentsViewModel(
            ucGetRealEstate, ucGetScpi, ucGetAirbnbByYear, ucGetVehicleRentals,
            ucGetCustomAssets, ucGetEmployeeSavings,
            ucSaveRealEstate, ucSaveScpi, ucSaveAirbnbRental, ucSaveVehicleEntry,
            ucSaveCustomAsset, ucSaveEmployeeSavings,
            ucUpdateRealEstate, ucUpdateScpi, ucUpdateAirbnbRental, ucUpdateVehicleEntry,
            ucUpdateCustomAsset, ucUpdateEmployeeSavings,
            ucDeleteRealEstate, ucDeleteScpi, ucDeleteAirbnbRental, ucDeleteVehicleEntry,
            ucDeleteCustomAsset, ucDeleteEmployeeSavings,
            ucSaveVersement, ucExisteVersementMois, ucGetPreferences, ratesRepo, ucGetDebts,
            ucGetPatrimoineHistory, ucCalculerTendance
        )

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is InvestmentsUiState.Success } as InvestmentsUiState.Success

        assertEquals(15000L, state.vehicleRentalRevenueCents)
        assertEquals(8000L, state.vehicleRentalChargeCents)
        job.cancel()
    }
}
