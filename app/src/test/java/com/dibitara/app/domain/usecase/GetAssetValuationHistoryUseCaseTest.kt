package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetAssetValuationHistoryUseCaseTest {

    private val repository: AssetValuationSnapshotRepository = mockk()
    private lateinit var useCase: GetAssetValuationHistoryUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetAssetValuationHistoryUseCase(repository)
    }

    @Test
    fun `repository vide - liste vide retournée`() = runTest {
        every { repository.getForAsset(AssetValuationType.REAL_ESTATE, 1L) } returns flowOf(emptyList())

        val result = useCase(AssetValuationType.REAL_ESTATE, 1L).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `plusieurs snapshots du même mois pour le même actif - un seul point retourné (le dernier)`() = runTest {
        val debut = buildSnapshot(id = 1L, date = LocalDate.of(2025, 3, 5), valueCents = 200_000_00L)
        val fin   = buildSnapshot(id = 2L, date = LocalDate.of(2025, 3, 20), valueCents = 210_000_00L)
        every { repository.getForAsset(AssetValuationType.REAL_ESTATE, 1L) } returns flowOf(listOf(debut, fin))

        val result = useCase(AssetValuationType.REAL_ESTATE, 1L).first()

        assertEquals(1, result.size)
        assertEquals(210_000_00L, result[0].valueCents)
    }

    @Test
    fun `snapshots sur 2 mois - 2 points triés chronologiquement`() = runTest {
        val mars  = buildSnapshot(id = 1L, date = LocalDate.of(2025, 3, 15), valueCents = 200_000_00L)
        val avril = buildSnapshot(id = 2L, date = LocalDate.of(2025, 4, 15), valueCents = 205_000_00L)
        every { repository.getForAsset(AssetValuationType.REAL_ESTATE, 1L) } returns flowOf(listOf(mars, avril))

        val result = useCase(AssetValuationType.REAL_ESTATE, 1L).first()

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2025, 3, 15), result[0].snapshotDate)
        assertEquals(LocalDate.of(2025, 4, 15), result[1].snapshotDate)
    }

    @Test
    fun `plus de 12 mois de données - limité aux 12 derniers mois`() = runTest {
        val snapshots = (1..15).map { mois ->
            buildSnapshot(id = mois.toLong(), date = LocalDate.of(2024, 1, 1).plusMonths(mois - 1L))
        }
        every { repository.getForAsset(AssetValuationType.SCPI, 1L) } returns flowOf(snapshots)

        val result = useCase(AssetValuationType.SCPI, 1L).first()

        assertEquals(12, result.size)
        assertEquals(LocalDate.of(2024, 4, 1), result[0].snapshotDate)
        assertEquals(LocalDate.of(2025, 3, 1), result.last().snapshotDate)
    }

    @Test
    fun `deux assetId différents ne se mélangent pas`() = runTest {
        val actif1 = buildSnapshot(id = 1L, date = LocalDate.of(2025, 3, 15), valueCents = 200_000_00L)
        every { repository.getForAsset(AssetValuationType.REAL_ESTATE, 1L) } returns flowOf(listOf(actif1))
        every { repository.getForAsset(AssetValuationType.REAL_ESTATE, 2L) } returns flowOf(emptyList())

        val result1 = useCase(AssetValuationType.REAL_ESTATE, 1L).first()
        val result2 = useCase(AssetValuationType.REAL_ESTATE, 2L).first()

        assertEquals(1, result1.size)
        assertTrue(result2.isEmpty())
    }

    private fun buildSnapshot(
        id: Long,
        date: LocalDate,
        valueCents: Long = 100_000_00L
    ) = AssetValuationSnapshot(
        id           = id,
        assetType    = AssetValuationType.REAL_ESTATE,
        assetId      = 1L,
        snapshotDate = date,
        valueCents   = valueCents,
        currency     = Currency.EUR
    )
}
