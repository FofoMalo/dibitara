package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetPatrimoineHistoryUseCaseTest {

    private val repository: PatrimoineSnapshotRepository = mockk()
    private lateinit var useCase: GetPatrimoineHistoryUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetPatrimoineHistoryUseCase(repository)
    }

    @Test
    fun `repository vide — liste vide retournée`() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `plusieurs snapshots du même mois — un seul point retourné (le dernier)`() = runTest {
        val debut = buildSnapshot(id = 1L, date = LocalDate.of(2025, 3, 5), netCents = 80_000_00L)
        val fin   = buildSnapshot(id = 2L, date = LocalDate.of(2025, 3, 20), netCents = 85_000_00L)
        every { repository.getAll() } returns flowOf(listOf(debut, fin))

        val result = useCase().first()

        assertEquals(1, result.size)
        // Garde le dernier snapshot du mois (date la plus récente = id 2)
        assertEquals(85_000_00L, result[0].patrimoineNetCents)
    }

    @Test
    fun `snapshots sur 2 mois — 2 points triés chronologiquement`() = runTest {
        val mars  = buildSnapshot(id = 1L, date = LocalDate.of(2025, 3, 15), netCents = 80_000_00L)
        val avril = buildSnapshot(id = 2L, date = LocalDate.of(2025, 4, 15), netCents = 82_000_00L)
        every { repository.getAll() } returns flowOf(listOf(mars, avril))

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2025, 3, 15), result[0].snapshotDate)
        assertEquals(LocalDate.of(2025, 4, 15), result[1].snapshotDate)
    }

    @Test
    fun `plus de 12 mois de données — limité aux 12 derniers mois`() = runTest {
        val snapshots = (1..15).map { mois ->
            buildSnapshot(id = mois.toLong(), date = LocalDate.of(2024, 1, 1).plusMonths(mois - 1L))
        }
        every { repository.getAll() } returns flowOf(snapshots)

        val result = useCase().first()

        assertEquals(12, result.size)
        // 15 mois depuis 2024-01 → takeLast(12) = mois 4 à 15 → 2024-04 à 2025-03
        assertEquals(LocalDate.of(2024, 4, 1), result[0].snapshotDate)
        assertEquals(LocalDate.of(2025, 3, 1), result.last().snapshotDate)
    }

    private fun buildSnapshot(
        id: Long,
        date: LocalDate,
        netCents: Long = 100_000_00L
    ) = PatrimoineSnapshot(
        id                  = id,
        snapshotDate        = date,
        patrimoineBrutCents = 150_000_00L,
        patrimoineNetCents  = netCents,
        currency            = Currency.EUR
    )
}
