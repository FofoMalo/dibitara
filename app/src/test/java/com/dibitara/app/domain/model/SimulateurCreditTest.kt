package com.dibitara.app.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [SimulateurCredit] - fonctions pures d'amortissement.
 */
class SimulateurCreditTest {

    // ─── simuler - cas invalides ──────────────────────────────────────────────

    @Test
    fun `simuler retourne null si le taux annuel est nul ou négatif`() {
        assertNull(SimulateurCredit.simuler(100_000_00L, 0.0, 500_00L, 10_000_00L))
        assertNull(SimulateurCredit.simuler(100_000_00L, -1.0, 500_00L, 10_000_00L))
    }

    @Test
    fun `simuler retourne null si la mensualité est nulle ou négative`() {
        assertNull(SimulateurCredit.simuler(100_000_00L, 1.85, 0L, 10_000_00L))
    }

    @Test
    fun `simuler retourne null si le capital restant est nul ou négatif`() {
        assertNull(SimulateurCredit.simuler(0L, 1.85, 500_00L, 10_000_00L))
    }

    @Test
    fun `simuler retourne null si la mensualité ne couvre pas les intérêts du premier mois`() {
        // Capital 100 000€, taux annuel 12% (1% par mois) → intérêts mensuels = 1 000€
        // Mensualité de 500€ ne couvre même pas les intérêts
        val result = SimulateurCredit.simuler(
            capitalRestantCents = 100_000_00L,
            tauxAnnuelPct = 12.0,
            mensualiteCents = 500_00L,
            remboursementAnticipeCents = 1_000_00L
        )
        assertNull(result)
    }

    // ─── simuler - cas nominal ─────────────────────────────────────────────────

    @Test
    fun `simuler calcule une durée restante plus courte après remboursement anticipé`() {
        val result = SimulateurCredit.simuler(
            capitalRestantCents = 100_000_00L,
            tauxAnnuelPct = 1.85,
            mensualiteCents = 900_00L,
            remboursementAnticipeCents = 20_000_00L,
            anneeDebutRemboursement = 2026,
            moisDebutRemboursement = 6
        )

        assertNotNull(result)
        result!!
        assertTrue(result.moisApres < result.moisActuels)
        assertEquals(result.moisActuels - result.moisApres, result.moisEconomises)
        assertTrue(result.interetsEconomisesCents >= 0L)
        assertNotNull(result.nouvelleEcheanceAnneeMois)
    }

    @Test
    fun `simuler retourne une échéance nulle quand le remboursement anticipé solde le crédit`() {
        val result = SimulateurCredit.simuler(
            capitalRestantCents = 10_000_00L,
            tauxAnnuelPct = 1.85,
            mensualiteCents = 900_00L,
            remboursementAnticipeCents = 10_000_00L,
            anneeDebutRemboursement = 2026,
            moisDebutRemboursement = 6
        )

        assertNotNull(result)
        result!!
        assertEquals(0, result.moisApres)
        assertEquals(0L, result.interetsTotauxApresCents)
        assertNull(result.nouvelleEcheanceAnneeMois)
    }

    @Test
    fun `simuler fait passer l échéance à l année suivante quand le mois dépasse décembre`() {
        val result = SimulateurCredit.simuler(
            capitalRestantCents = 100_000_00L,
            tauxAnnuelPct = 1.85,
            mensualiteCents = 900_00L,
            remboursementAnticipeCents = 20_000_00L,
            anneeDebutRemboursement = 2026,
            moisDebutRemboursement = 11
        )

        assertNotNull(result)
        result!!
        val (annee, mois) = result.nouvelleEcheanceAnneeMois!!
        assertTrue(mois in 1..12)
        assertTrue(annee >= 2026)
    }

    // ─── calculerMensualite ────────────────────────────────────────────────────

    @Test
    fun `calculerMensualite retourne zéro si la durée est nulle ou négative`() {
        assertEquals(0L, SimulateurCredit.calculerMensualite(100_000_00L, 1.85, 0))
        assertEquals(0L, SimulateurCredit.calculerMensualite(100_000_00L, 1.85, -12))
    }

    @Test
    fun `calculerMensualite retourne zéro si le taux annuel est négatif`() {
        assertEquals(0L, SimulateurCredit.calculerMensualite(100_000_00L, -1.0, 240))
    }

    @Test
    fun `calculerMensualite à taux zéro divise simplement le capital par la durée`() {
        val mensualite = SimulateurCredit.calculerMensualite(120_000_00L, 0.0, 120)

        assertEquals(1_000_00L, mensualite)
    }

    @Test
    fun `calculerMensualite à taux positif calcule une mensualité cohérente`() {
        val mensualite = SimulateurCredit.calculerMensualite(200_000_00L, 1.85, 240)

        // Une mensualité positive et raisonnable par rapport au capital
        assertTrue(mensualite > 0L)
        assertTrue(mensualite < 200_000_00L)
    }
}
