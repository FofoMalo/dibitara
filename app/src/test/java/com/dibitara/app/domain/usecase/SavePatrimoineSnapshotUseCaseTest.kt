package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SavePatrimoineSnapshotUseCaseTest {

    private val repository: PatrimoineSnapshotRepository = mockk()
    private lateinit var useCase: SavePatrimoineSnapshotUseCase

    private val overview = PatrimonyOverview(
        liquiditesCents          = 5_000_00L,
        epargneCents             = 20_000_00L,
        investissementsCents     = 100_000_00L,
        airbnbAnnualRevenueCents = 0L,
        vehicleRentalNetRevenueCents = 0L,
        dettesTotalCents         = 50_000_00L,
        currency                 = Currency.EUR
    )

    @BeforeEach
    fun setUp() {
        useCase = SavePatrimoineSnapshotUseCase(repository)
    }

    @Test
    fun `sauvegarde un snapshot si aucun n'existe aujourd'hui`() = runTest {
        coEvery { repository.existsForDay(any()) } returns false
        coEvery { repository.save(any()) } returns Unit

        val result = useCase(overview)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `ne sauvegarde pas si un snapshot existe déjà aujourd'hui`() = runTest {
        coEvery { repository.existsForDay(any()) } returns true

        val result = useCase(overview)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `le snapshot sauvegardé contient les bonnes valeurs`() = runTest {
        val snapshotCapture = mutableListOf<PatrimoineSnapshot>()
        coEvery { repository.existsForDay(any()) } returns false
        coEvery { repository.save(capture(snapshotCapture)) } returns Unit

        useCase(overview)

        val saved = snapshotCapture.first()
        assertEquals(overview.patrimoineBrutCents, saved.patrimoineBrutCents)
        assertEquals(overview.patrimoineNetCents,  saved.patrimoineNetCents)
        assertEquals(Currency.EUR, saved.currency)
        assertEquals(LocalDate.now(), saved.snapshotDate)
    }
}
