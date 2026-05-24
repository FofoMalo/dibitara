package com.dibitara.app.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [CurrencyConverter].
 * On utilise des taux fixes (1 € = 1,10 $ et 1 € = 660 FCFA)
 * pour des vérifications exactes en centimes.
 */
class CurrencyConverterTest {

    // Taux de test simples — différents des valeurs réelles pour détecter une inversion de ratio
    private val rates = ExchangeRates(
        usdParEur  = 1.10,   // 1 € = 1,10 $
        xofParEur  = 660.0,  // 1 € = 660 FCFA
        horodatage = 0L
    )

    // ─── Identité ─────────────────────────────────────────────────────────────

    @Test
    fun `même devise retourne le montant inchangé`() {
        assertEquals(1000L, CurrencyConverter.convertCents(1000L, Currency.EUR, Currency.EUR, rates))
        assertEquals(5000L, CurrencyConverter.convertCents(5000L, Currency.USD, Currency.USD, rates))
    }

    @Test
    fun `XOF vers XAF retourne le montant inchangé — parité 1 à 1`() {
        assertEquals(66000L, CurrencyConverter.convertCents(66000L, Currency.XOF, Currency.XAF, rates))
    }

    @Test
    fun `XAF vers XOF retourne le montant inchangé — parité 1 à 1`() {
        assertEquals(66000L, CurrencyConverter.convertCents(66000L, Currency.XAF, Currency.XOF, rates))
    }

    // ─── EUR ↔ USD ────────────────────────────────────────────────────────────

    @Test
    fun `EUR vers USD — multiplie par le taux`() {
        // 100 € = 100 * 1,10 $ = 110 $  →  10000 centimes * 1.10 = 11000
        val result = CurrencyConverter.convertCents(10_000L, Currency.EUR, Currency.USD, rates)
        assertEquals(11_000L, result)
    }

    @Test
    fun `USD vers EUR — divise par le taux`() {
        // 110 $ = 100 €  →  11000 centimes / 1.10 = 10000
        val result = CurrencyConverter.convertCents(11_000L, Currency.USD, Currency.EUR, rates)
        assertEquals(10_000L, result)
    }

    // ─── EUR ↔ XOF ────────────────────────────────────────────────────────────

    @Test
    fun `EUR vers XOF — multiplie par le taux`() {
        // 1 € = 660 FCFA  →  100 centimes EUR * 660 = 66000 centimes XOF
        val result = CurrencyConverter.convertCents(100L, Currency.EUR, Currency.XOF, rates)
        assertEquals(66_000L, result)
    }

    @Test
    fun `XOF vers EUR — divise par le taux`() {
        // 660 FCFA = 1 €  →  66000 centimes XOF / 660 = 100 centimes EUR
        val result = CurrencyConverter.convertCents(66_000L, Currency.XOF, Currency.EUR, rates)
        assertEquals(100L, result)
    }

    // ─── Conversion croisée USD ↔ XOF (via EUR pivot) ─────────────────────────

    @Test
    fun `USD vers XOF passe par EUR pivot`() {
        // 1,10 $ = 1 € = 660 FCFA
        // 11000 centimes USD → 10000 centimes EUR → 6 600 000 centimes XOF
        val result = CurrencyConverter.convertCents(11_000L, Currency.USD, Currency.XOF, rates)
        assertEquals(6_600_000L, result)
    }

    @Test
    fun `XAF vers USD utilise la parité XAF-XOF puis passe par EUR`() {
        // 66000 centimes XAF = 66000 XOF → 100 EUR → 110 USD
        val result = CurrencyConverter.convertCents(66_000L, Currency.XAF, Currency.USD, rates)
        assertEquals(110L, result)
    }
}
