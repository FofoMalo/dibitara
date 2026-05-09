package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvestmentUseCasesTest {

    private val repository: InvestmentRepository = mockk()

    private fun buildRealEstate(label: String = "Appart Lyon", value: Long = 200000L) =
        RealEstateAsset(label = label, currentValueCents = value, currency = Currency.EUR, updatedAt = LocalDate.now())

    private fun buildScpi(label: String = "SCPI Primovie", shares: Int = 10) =
        ScpiInvestment(label = label, sharesCount = shares, shareValueCents = 20000L,
            monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.now())

    private fun buildAirbnb(label: String = "Studio Bordeaux", amount: Long = 90000L) =
        AirbnbRental(propertyLabel = label, amountCents = amount, date = LocalDate.now(), currency = Currency.EUR)

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
        assertTrue(SaveScpiUseCase(repository)(buildScpi(shares = 0)).isFailure)
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
    fun `DeleteRealEstate délègue au repository`() = runTest {
        val asset = buildRealEstate()
        coJustRun { repository.deleteRealEstate(asset) }
        DeleteRealEstateUseCase(repository)(asset)
        coVerify { repository.deleteRealEstate(asset) }
    }

    // ─── DeleteScpiUseCase ───────────────────────────────────────────────────

    @Test
    fun `DeleteScpi délègue au repository`() = runTest {
        val scpi = buildScpi()
        coJustRun { repository.deleteScpi(scpi) }
        DeleteScpiUseCase(repository)(scpi)
        coVerify { repository.deleteScpi(scpi) }
    }

    // ─── DeleteAirbnbRentalUseCase ───────────────────────────────────────────

    @Test
    fun `DeleteAirbnbRental délègue au repository`() = runTest {
        val rental = buildAirbnb()
        coJustRun { repository.deleteAirbnbRental(rental) }
        DeleteAirbnbRentalUseCase(repository)(rental)
        coVerify { repository.deleteAirbnbRental(rental) }
    }
}
