package com.dibitara.app.data.importcsv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [MontantParser]. Objet Kotlin pur, aucune dépendance.
 */
class MontantParserTest {

    // ─── parse() avec séparateur décimal connu ────────────────────────────────

    @Test
    fun `notation FR avec séparateur de milliers espace`() {
        assertEquals(1234.56, MontantParser.parse("1 234,56", ','))
    }

    @Test
    fun `notation FR négative`() {
        assertEquals(-120.50, MontantParser.parse("-120,50", ','))
    }

    @Test
    fun `notation FR avec espace insécable comme séparateur de milliers`() {
        assertEquals(12345.67, MontantParser.parse("12 345,67", ','))
    }

    @Test
    fun `tiret long unicode traité comme signe négatif`() {
        assertEquals(-45.99, MontantParser.parse("−45,99", ','))
    }

    @Test
    fun `notation EN avec séparateur de milliers virgule`() {
        assertEquals(1234.56, MontantParser.parse("1,234.56", '.'))
    }

    @Test
    fun `parenthèses comptables valent un montant négatif`() {
        assertEquals(-45.99, MontantParser.parse("(45,99)", ','))
    }

    @Test
    fun `symbole de devise et espaces parasites sont ignorés`() {
        assertEquals(45.99, MontantParser.parse("  45,99 € ", ','))
        assertEquals(1200.0, MontantParser.parse("EUR 1 200,00", ','))
    }

    @Test
    fun `entier sans décimale`() {
        assertEquals(50.0, MontantParser.parse("50", ','))
        assertEquals(-1500.0, MontantParser.parse("-1 500", ','))
    }

    @Test
    fun `chaîne vide ou non numérique retourne null`() {
        assertNull(MontantParser.parse("", ','))
        assertNull(MontantParser.parse("   ", ','))
        assertNull(MontantParser.parse("N/A", ','))
        assertNull(MontantParser.parse("-", ','))
    }

    // ─── detecterSeparateurDecimal() ──────────────────────────────────────────

    @Test
    fun `détecte la virgule décimale`() {
        assertEquals(',', MontantParser.detecterSeparateurDecimal("45,99"))
        assertEquals(',', MontantParser.detecterSeparateurDecimal("1 234,56"))
        assertEquals(',', MontantParser.detecterSeparateurDecimal("1.234,56"))
    }

    @Test
    fun `détecte le point décimal`() {
        assertEquals('.', MontantParser.detecterSeparateurDecimal("45.99"))
        assertEquals('.', MontantParser.detecterSeparateurDecimal("1,234.56"))
    }

    @Test
    fun `un seul séparateur suivi de 3 chiffres est un séparateur de milliers`() {
        // "1.234" → le point regroupe les milliers, la vraie décimale est la virgule (absente ici)
        assertEquals(',', MontantParser.detecterSeparateurDecimal("1.234"))
        // "1,234" → la virgule regroupe les milliers (notation EN)
        assertEquals('.', MontantParser.detecterSeparateurDecimal("1,234"))
    }
}
