package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CalculerTendanceActifUseCaseTest {

    private val useCase = CalculerTendanceActifUseCase()

    private fun snapshot(date: LocalDate, valueCents: Long) = AssetValuationSnapshot(
        assetType = AssetValuationType.REAL_ESTATE, assetId = 1L,
        snapshotDate = date, valueCents = valueCents, currency = Currency.EUR
    )

    @Test
    fun `moins de 2 points - retourne null`() {
        assertNull(useCase(emptyList()))
        assertNull(useCase(listOf(snapshot(LocalDate.of(2025, 1, 1), 200_000_00L))))
    }

    @Test
    fun `premier point à 0 - retourne null (évite la division par zéro)`() {
        val history = listOf(
            snapshot(LocalDate.of(2025, 1, 1), 0L),
            snapshot(LocalDate.of(2025, 2, 1), 100_000_00L)
        )
        assertNull(useCase(history))
    }

    @Test
    fun `hausse - pourcentage positif calculé correctement`() {
        val history = listOf(
            snapshot(LocalDate.of(2025, 1, 1), 200_000_00L),
            snapshot(LocalDate.of(2025, 12, 1), 220_000_00L)
        )
        assertEquals(10f, useCase(history)!!, 0.01f)
    }

    @Test
    fun `baisse - pourcentage négatif calculé correctement`() {
        val history = listOf(
            snapshot(LocalDate.of(2025, 1, 1), 200_000_00L),
            snapshot(LocalDate.of(2025, 12, 1), 180_000_00L)
        )
        assertEquals(-10f, useCase(history)!!, 0.01f)
    }
}
