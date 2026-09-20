package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SpendingRecommendation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckRevenuIncompletUseCaseTest {

    private val getSpendingRecommendations: GetSpendingRecommendationsUseCase = mockk()
    private val useCase = CheckRevenuIncompletUseCase(getSpendingRecommendations)

    private val today = LocalDate.of(2026, 5, 15)

    private fun recommandation(revenuMoyen: Long, revenuParMois: List<Long>) = SpendingRecommendation(
        currency               = Currency.EUR,
        revenuMoyenCents       = revenuMoyen,
        revenuParMoisCents     = revenuParMois,
        depensesMoyennesCents  = 0L,
        engagementsMensuels    = 0L,
        tauxEpargneActuelPct   = null,
        tauxEpargneCiblePct    = 20,
        objectifEpargneCents   = 0L,
        pouchesRecommandees    = emptyList(),
        soldePrevisionelCents  = 0L,
        estEquilibre           = true,
        moisDeReference        = listOf(4 to 2026, 3 to 2026, 2 to 2026)
    )

    @Test
    fun `retourne l alerte quand le revenu du mois le plus récent est sous 30% de la moyenne`() = runTest {
        // Cas réel du cadrage : 2213,19€ contre une moyenne de 13544,04€ (≈16%)
        every { getSpendingRecommendations(5, 2026) } returns flowOf(
            recommandation(revenuMoyen = 1_354_404L, revenuParMois = listOf(221_319L, 2_000_000L, 1_841_893L))
        )

        val result = useCase(today)

        assertNotNull(result)
        assertEquals(4, result!!.mois)
        assertEquals(2026, result.annee)
        assertEquals(221_319L, result.revenuMoisCents)
        assertEquals(1_354_404L, result.revenuMoyenCents)
    }

    @Test
    fun `retourne null quand le revenu du mois le plus récent est au-dessus du seuil`() = runTest {
        every { getSpendingRecommendations(5, 2026) } returns flowOf(
            recommandation(revenuMoyen = 200_000L, revenuParMois = listOf(180_000L, 200_000L, 220_000L))
        )

        val result = useCase(today)

        assertNull(result)
    }

    @Test
    fun `retourne null quand la moyenne est nulle`() = runTest {
        every { getSpendingRecommendations(5, 2026) } returns flowOf(
            recommandation(revenuMoyen = 0L, revenuParMois = listOf(0L, 0L, 0L))
        )

        val result = useCase(today)

        assertNull(result)
    }

    @Test
    fun `retourne null exactement au seuil de 30%`() = runTest {
        every { getSpendingRecommendations(5, 2026) } returns flowOf(
            recommandation(revenuMoyen = 100_000L, revenuParMois = listOf(30_000L, 100_000L, 170_000L))
        )

        val result = useCase(today)

        assertNull(result)
    }
}
