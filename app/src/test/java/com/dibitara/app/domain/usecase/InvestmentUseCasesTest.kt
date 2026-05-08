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

    // ─── GetInvestmentsUseCase ───────────────────────────────────────────────

    @Test
    fun `GetInvestments realEstate délègue au repository`() {
        every { repository.getAllRealEstate() } returns flowOf(emptyList())
        assertNotNull(GetInvestmentsUseCase(repository).realEstate())
    }

    @Test
    fun `GetInvestments scpi délègue au repository`() {
        every { repository.getAllScpi() } returns flowOf(emptyList())
        assertNotNull(GetInvestmentsUseCase(repository).scpi())
    }

    @Test
    fun `GetInvestments airbnb délègue au repository`() {
        every { repository.getAllAirbnbRentals() } returns flowOf(emptyList())
        assertNotNull(GetInvestmentsUseCase(repository).airbnb())
    }

    @Test
    fun `GetInvestments airbnbByYear filtre par année`() {
        every { repository.getAirbnbRentalsByYear(2026) } returns flowOf(emptyList())
        assertNotNull(GetInvestmentsUseCase(repository).airbnbByYear(2026))
    }

    // ─── SaveInvestmentUseCase ───────────────────────────────────────────────

    @Test
    fun `SaveInvestment realEstate retourne succès`() = runTest {
        val asset = buildRealEstate()
        coEvery { repository.saveRealEstate(asset) } returns Result.success(1L)
        assertTrue(SaveInvestmentUseCase(repository).realEstate(asset).isSuccess)
    }

    @Test
    fun `SaveInvestment realEstate retourne échec si libellé vide`() = runTest {
        assertTrue(SaveInvestmentUseCase(repository).realEstate(buildRealEstate(label = "")).isFailure)
    }

    @Test
    fun `SaveInvestment realEstate retourne échec si valeur nulle`() = runTest {
        assertTrue(SaveInvestmentUseCase(repository).realEstate(buildRealEstate(value = 0L)).isFailure)
    }

    @Test
    fun `SaveInvestment scpi retourne succès`() = runTest {
        val scpi = buildScpi()
        coEvery { repository.saveScpi(scpi) } returns Result.success(1L)
        assertTrue(SaveInvestmentUseCase(repository).scpi(scpi).isSuccess)
    }

    @Test
    fun `SaveInvestment scpi retourne échec si parts nulles`() = runTest {
        assertTrue(SaveInvestmentUseCase(repository).scpi(buildScpi(shares = 0)).isFailure)
    }

    @Test
    fun `SaveInvestment airbnb retourne succès`() = runTest {
        val rental = buildAirbnb()
        coEvery { repository.saveAirbnbRental(rental) } returns Result.success(1L)
        assertTrue(SaveInvestmentUseCase(repository).airbnb(rental).isSuccess)
    }

    @Test
    fun `SaveInvestment airbnb retourne échec si montant nul`() = runTest {
        assertTrue(SaveInvestmentUseCase(repository).airbnb(buildAirbnb(amount = 0L)).isFailure)
    }

    // ─── DeleteInvestmentUseCase ─────────────────────────────────────────────

    @Test
    fun `DeleteInvestment realEstate délègue au repository`() = runTest {
        val asset = buildRealEstate()
        coJustRun { repository.deleteRealEstate(asset) }
        DeleteInvestmentUseCase(repository).realEstate(asset)
        coVerify { repository.deleteRealEstate(asset) }
    }

    @Test
    fun `DeleteInvestment scpi délègue au repository`() = runTest {
        val scpi = buildScpi()
        coJustRun { repository.deleteScpi(scpi) }
        DeleteInvestmentUseCase(repository).scpi(scpi)
        coVerify { repository.deleteScpi(scpi) }
    }

    @Test
    fun `DeleteInvestment airbnb délègue au repository`() = runTest {
        val rental = buildAirbnb()
        coJustRun { repository.deleteAirbnbRental(rental) }
        DeleteInvestmentUseCase(repository).airbnb(rental)
        coVerify { repository.deleteAirbnbRental(rental) }
    }
}
