package com.dibitara.app.data.importcsv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [CsvParser] : découpage RFC 4180 et détection du délimiteur.
 */
class CsvParserTest {

    // ─── parseLigne() ─────────────────────────────────────────────────────────

    @Test
    fun `découpe une ligne simple`() {
        assertEquals(
            listOf("01/02/2026", "Courses", "-42,90"),
            CsvParser.parseLigne("01/02/2026;Courses;-42,90", ';')
        )
    }

    @Test
    fun `un délimiteur entre guillemets ne coupe pas le champ`() {
        assertEquals(
            listOf("01/02/2026", "PAIEMENT CB CARREFOUR; MARKET", "-42,90"),
            CsvParser.parseLigne("""01/02/2026;"PAIEMENT CB CARREFOUR; MARKET";-42,90""", ';')
        )
    }

    @Test
    fun `guillemets doublés à l'intérieur d'une valeur`() {
        assertEquals(
            listOf("Virement de \"Jean\""),
            CsvParser.parseLigne("\"Virement de \"\"Jean\"\"\"", ';')
        )
    }

    @Test
    fun `champ vide final conservé quand la ligne se termine par le délimiteur`() {
        assertEquals(listOf("a", "b", ""), CsvParser.parseLigne("a;b;", ';'))
    }

    @Test
    fun `champ vide au milieu`() {
        assertEquals(listOf("a", "", "c"), CsvParser.parseLigne("a;;c", ';'))
    }

    @Test
    fun `ligne vide retourne une liste vide`() {
        assertEquals(emptyList<String>(), CsvParser.parseLigne("", ';'))
    }

    @Test
    fun `découpe avec une tabulation comme délimiteur`() {
        assertEquals(
            listOf("2026-02-01", "Salaire", "2500.00"),
            CsvParser.parseLigne("2026-02-01\tSalaire\t2500.00", '\t')
        )
    }

    // ─── detecterDelimiteur() ─────────────────────────────────────────────────

    @Test
    fun `détecte le point-virgule`() {
        val echantillon = listOf(
            "Date;Libellé;Montant",
            "01/02/2026;Courses;-42,90",
            "03/02/2026;Salaire;2500,00"
        )
        assertEquals(';', CsvParser.detecterDelimiteur(echantillon))
    }

    @Test
    fun `détecte la virgule`() {
        val echantillon = listOf(
            "Date,Description,Amount",
            "2026-02-01,Groceries,-42.90",
            "2026-02-03,Salary,2500.00"
        )
        assertEquals(',', CsvParser.detecterDelimiteur(echantillon))
    }

    @Test
    fun `détecte la tabulation`() {
        val echantillon = listOf(
            "Date\tLibellé\tMontant",
            "01/02/2026\tCourses\t-42,90"
        )
        assertEquals('\t', CsvParser.detecterDelimiteur(echantillon))
    }

    @Test
    fun `une virgule décimale ne fait pas passer le fichier pour du CSV virgule`() {
        // Délimiteur ';' réel, montants en notation FR (virgule décimale).
        // Le candidat ',' découperait un nombre de colonnes irrégulier → écarté.
        val echantillon = listOf(
            "Date;Libellé;Montant",
            "01/02/2026;Boulangerie;-4,20",
            "02/02/2026;Restaurant du coin, Paris;-31,50",
            "03/02/2026;Salaire;2500,00"
        )
        assertEquals(';', CsvParser.detecterDelimiteur(echantillon))
    }
}
