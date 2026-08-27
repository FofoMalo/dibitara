package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvestmentUseCasesTest {

    private val repository: InvestmentRepository = mockk()
    private val snapshotRepository: AssetValuationSnapshotRepository = mockk(relaxed = true)

    private fun buildRealEstate(label: String = "Appart Lyon", value: Long = 200000L) =
        RealEstateAsset(label = label, currentValueCents = value, currency = Currency.EUR, updatedAt = LocalDate.now())

    private fun buildScpi(label: String = "SCPI Primovie", shares: Double = 10.0) =
        ScpiInvestment(label = label, sharesCount = shares, shareValueCents = 20000L,
            monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.now())

    private fun buildAirbnb(label: String = "Studio Bordeaux", amount: Long = 90000L) =
        AirbnbRental(propertyLabel = label, amountCents = amount, date = LocalDate.now(), currency = Currency.EUR)

    private fun buildVehicleEntry(label: String = "Location weekend", amount: Long = 15000L, type: VehicleEntryType = VehicleEntryType.REVENU) =
        VehicleRentalEntry(label = label, entryType = type, amountCents = amount, date = LocalDate.now(), currency = Currency.EUR)

    // ─── GetRealEstateUseCase ────────────────────────────────────────────────

    @Test
    fun `GetRealEstate délègue au repository`() {
        every { repository.getAllRealEstate() } returns flowOf(emptyList())
        assertNotNull(GetRealEstateUseCase(repository)())
    }

    // ─── GetScpiUseCase ──────────────────────────────────────────────────────

    @Test
    fun `GetScpi délègue au repository`() {
        every { repository.getAllScpi() } returns flowOf(emptyList())
        assertNotNull(GetScpiUseCase(repository)())
    }

    // ─── GetAirbnbRentalsUseCase ─────────────────────────────────────────────

    @Test
    fun `GetAirbnbRentals délègue au repository`() {
        every { repository.getAllAirbnbRentals() } returns flowOf(emptyList())
        assertNotNull(GetAirbnbRentalsUseCase(repository)())
    }

    // ─── GetAirbnbRentalsByYearUseCase ───────────────────────────────────────

    @Test
    fun `GetAirbnbRentalsByYear filtre par année`() {
        every { repository.getAirbnbRentalsByYear(2026) } returns flowOf(emptyList())
        assertNotNull(GetAirbnbRentalsByYearUseCase(repository)(2026))
    }

    // ─── SaveRealEstateUseCase ───────────────────────────────────────────────

    @Test
    fun `SaveRealEstate retourne succès`() = runTest {
        val asset = buildRealEstate()
        coEvery { repository.saveRealEstate(asset) } returns Result.success(1L)
        assertTrue(SaveRealEstateUseCase(repository)(asset).isSuccess)
    }

    @Test
    fun `SaveRealEstate retourne échec si libellé vide`() = runTest {
        assertTrue(SaveRealEstateUseCase(repository)(buildRealEstate(label = "")).isFailure)
    }

    @Test
    fun `SaveRealEstate retourne échec si valeur nulle`() = runTest {
        assertTrue(SaveRealEstateUseCase(repository)(buildRealEstate(value = 0L)).isFailure)
    }

    // ─── SaveScpiUseCase ─────────────────────────────────────────────────────

    @Test
    fun `SaveScpi retourne succès`() = runTest {
        val scpi = buildScpi()
        coEvery { repository.saveScpi(scpi) } returns Result.success(1L)
        assertTrue(SaveScpiUseCase(repository)(scpi).isSuccess)
    }

    @Test
    fun `SaveScpi retourne échec si parts nulles`() = runTest {
        assertTrue(SaveScpiUseCase(repository)(buildScpi(shares = 0.0)).isFailure)
    }

    // ─── SaveAirbnbRentalUseCase ─────────────────────────────────────────────

    @Test
    fun `SaveAirbnbRental retourne succès`() = runTest {
        val rental = buildAirbnb()
        coEvery { repository.saveAirbnbRental(rental) } returns Result.success(1L)
        assertTrue(SaveAirbnbRentalUseCase(repository)(rental).isSuccess)
    }

    @Test
    fun `SaveAirbnbRental retourne échec si montant nul`() = runTest {
        assertTrue(SaveAirbnbRentalUseCase(repository)(buildAirbnb(amount = 0L)).isFailure)
    }

    // ─── DeleteRealEstateUseCase ─────────────────────────────────────────────

    @Test
    fun `DeleteRealEstate délègue au repository et purge l'historique de valorisation`() = runTest {
        val asset = buildRealEstate()
        coJustRun { repository.deleteRealEstate(asset) }
        DeleteRealEstateUseCase(repository, snapshotRepository)(asset)
        coVerify { repository.deleteRealEstate(asset) }
        coVerify { snapshotRepository.deleteForAsset(AssetValuationType.REAL_ESTATE, asset.id) }
    }

    // ─── DeleteScpiUseCase ───────────────────────────────────────────────────

    @Test
    fun `DeleteScpi délègue au repository et purge l'historique de valorisation`() = runTest {
        val scpi = buildScpi()
        coJustRun { repository.deleteScpi(scpi) }
        DeleteScpiUseCase(repository, snapshotRepository)(scpi)
        coVerify { repository.deleteScpi(scpi) }
        coVerify { snapshotRepository.deleteForAsset(AssetValuationType.SCPI, scpi.id) }
    }

    // ─── DeleteAirbnbRentalUseCase ───────────────────────────────────────────

    @Test
    fun `DeleteAirbnbRental délègue au repository`() = runTest {
        val rental = buildAirbnb()
        coJustRun { repository.deleteAirbnbRental(rental) }
        DeleteAirbnbRentalUseCase(repository)(rental)
        coVerify { repository.deleteAirbnbRental(rental) }
    }

    // ─── GetVehicleRentalEntriesUseCase ──────────────────────────────────────

    @Test
    fun `GetVehicleRentalEntries délègue au repository`() {
        every { repository.getAllVehicleRentalEntries() } returns flowOf(emptyList())
        assertNotNull(GetVehicleRentalEntriesUseCase(repository)())
    }

    // ─── GetVehicleRentalEntriesByYearUseCase ────────────────────────────────

    @Test
    fun `GetVehicleRentalEntriesByYear filtre par année`() {
        every { repository.getVehicleRentalEntriesByYear(2026) } returns flowOf(emptyList())
        assertNotNull(GetVehicleRentalEntriesByYearUseCase(repository)(2026))
    }

    // ─── SaveVehicleRentalEntryUseCase ───────────────────────────────────────

    @Test
    fun `SaveVehicleRentalEntry retourne succès pour un revenu`() = runTest {
        val entry = buildVehicleEntry(type = VehicleEntryType.REVENU)
        coEvery { repository.saveVehicleRentalEntry(entry) } returns Result.success(1L)
        assertTrue(SaveVehicleRentalEntryUseCase(repository)(entry).isSuccess)
    }

    @Test
    fun `SaveVehicleRentalEntry retourne succès pour une charge`() = runTest {
        val entry = buildVehicleEntry(type = VehicleEntryType.CHARGE)
        coEvery { repository.saveVehicleRentalEntry(entry) } returns Result.success(1L)
        assertTrue(SaveVehicleRentalEntryUseCase(repository)(entry).isSuccess)
    }

    @Test
    fun `SaveVehicleRentalEntry retourne échec si libellé vide`() = runTest {
        assertTrue(SaveVehicleRentalEntryUseCase(repository)(buildVehicleEntry(label = "")).isFailure)
    }

    @Test
    fun `SaveVehicleRentalEntry retourne échec si montant nul`() = runTest {
        assertTrue(SaveVehicleRentalEntryUseCase(repository)(buildVehicleEntry(amount = 0L)).isFailure)
    }

    // ─── UpdateVehicleRentalEntryUseCase ─────────────────────────────────────

    @Test
    fun `UpdateVehicleRentalEntry retourne succès`() = runTest {
        val entry = buildVehicleEntry()
        coJustRun { repository.updateVehicleRentalEntry(entry) }
        assertTrue(UpdateVehicleRentalEntryUseCase(repository)(entry).isSuccess)
    }

    @Test
    fun `UpdateVehicleRentalEntry retourne échec si montant nul`() = runTest {
        assertTrue(UpdateVehicleRentalEntryUseCase(repository)(buildVehicleEntry(amount = 0L)).isFailure)
    }

    // ─── DeleteVehicleRentalEntryUseCase ─────────────────────────────────────

    @Test
    fun `DeleteVehicleRentalEntry délègue au repository`() = runTest {
        val entry = buildVehicleEntry()
        coJustRun { repository.deleteVehicleRentalEntry(entry) }
        DeleteVehicleRentalEntryUseCase(repository)(entry)
        coVerify { repository.deleteVehicleRentalEntry(entry) }
    }
}
