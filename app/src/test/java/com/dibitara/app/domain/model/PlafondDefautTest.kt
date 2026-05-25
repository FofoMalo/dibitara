package com.dibitara.app.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlafondDefautTest {

    // ─── Plafonds légaux EUR connus ──────────────────────────────────────────

    @Test
    fun `Livret A EUR retourne 22 950 euros en centimes`() {
        assertEquals(2_295_000L, PlafondDefaut.suggerer(SavingsType.LIVRET_A, Currency.EUR))
    }

    @Test
    fun `LDDS EUR retourne 12 000 euros en centimes`() {
        assertEquals(1_200_000L, PlafondDefaut.suggerer(SavingsType.LDDS, Currency.EUR))
    }

    @Test
    fun `PEL EUR retourne 61 200 euros en centimes`() {
        assertEquals(6_120_000L, PlafondDefaut.suggerer(SavingsType.PEL, Currency.EUR))
    }

    @Test
    fun `PEA EUR retourne 150 000 euros en centimes`() {
        assertEquals(15_000_000L, PlafondDefaut.suggerer(SavingsType.PEA, Currency.EUR))
    }

    // ─── Plafond Orange Money XOF ────────────────────────────────────────────

    @Test
    fun `Orange Money XOF retourne 2 000 000 XOF en centimes`() {
        assertEquals(200_000_000L, PlafondDefaut.suggerer(SavingsType.ORANGE_MONEY, Currency.XOF))
    }

    // ─── Cas sans plafond connu ───────────────────────────────────────────────

    @Test
    fun `Assurance vie EUR retourne null (pas de plafond légal)`() {
        assertNull(PlafondDefaut.suggerer(SavingsType.ASSURANCE_VIE, Currency.EUR))
    }

    @Test
    fun `PER EUR retourne null`() {
        assertNull(PlafondDefaut.suggerer(SavingsType.PER, Currency.EUR))
    }

    @Test
    fun `AUTRE EUR retourne null`() {
        assertNull(PlafondDefaut.suggerer(SavingsType.AUTRE, Currency.EUR))
    }

    // ─── Mauvaise devise : pas de suggestion croisée ─────────────────────────

    @Test
    fun `Livret A en XOF retourne null (plafond Livret A uniquement pour EUR)`() {
        assertNull(PlafondDefaut.suggerer(SavingsType.LIVRET_A, Currency.XOF))
    }

    @Test
    fun `Orange Money en EUR retourne null`() {
        assertNull(PlafondDefaut.suggerer(SavingsType.ORANGE_MONEY, Currency.EUR))
    }
}
