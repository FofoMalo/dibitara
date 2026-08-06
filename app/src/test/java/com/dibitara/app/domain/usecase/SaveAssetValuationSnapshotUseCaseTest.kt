package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaveAssetValuationSnapshotUseCaseTest {

    private val repository: AssetValuationSnapshotRepository = mockk()
    private lateinit var useCase: SaveAssetValuationSnapshotUseCase

    @BeforeEach
    fun setUp() {
        useCase = SaveAssetValuationSnapshotUseCase(repository)
    }

    @Test
    fun `sauvegarde un snapshot si aucun n'existe aujourd'hui pour cet actif`() = runTest {
        coEvery { repository.existsForDay(AssetValuationType.REAL_ESTATE, 1L, any()) } returns false
        coEvery { repository.save(any()) } returns Unit

        val result = useCase(AssetValuationType.REAL_ESTATE, 1L, 250_000_00L, Currency.EUR)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `ne sauvegarde pas si un snapshot existe déjà aujourd'hui pour cet actif`() = runTest {
        coEvery { repository.existsForDay(AssetValuationType.SCPI, 2L, any()) } returns true

        val result = useCase(AssetValuationType.SCPI, 2L, 20_000_00L, Currency.EUR)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `le snapshot sauvegardé contient les bonnes valeurs`() = runTest {
        val snapshotCapture = mutableListOf<AssetValuationSnapshot>()
        coEvery { repository.existsForDay(AssetValuationType.REAL_ESTATE, 1L, any()) } returns false
        coEvery { repository.save(capture(snapshotCapture)) } returns Unit

        useCase(AssetValuationType.REAL_ESTATE, 1L, 250_000_00L, Currency.EUR)

        val saved = snapshotCapture.first()
        assertEquals(AssetValuationType.REAL_ESTATE, saved.assetType)
        assertEquals(1L, saved.assetId)
        assertEquals(250_000_00L, saved.valueCents)
        assertEquals(Currency.EUR, saved.currency)
        assertEquals(LocalDate.now(), saved.snapshotDate)
    }

    @Test
    fun `un même actif ne pollue pas la garde-fou d'un autre actif du même type`() = runTest {
        coEvery { repository.existsForDay(AssetValuationType.SCPI, 1L, any()) } returns true
        coEvery { repository.existsForDay(AssetValuationType.SCPI, 2L, any()) } returns false
        coEvery { repository.save(any()) } returns Unit

        useCase(AssetValuationType.SCPI, 2L, 10_000_00L, Currency.EUR)

        coVerify(exactly = 1) { repository.save(any()) }
    }
}
