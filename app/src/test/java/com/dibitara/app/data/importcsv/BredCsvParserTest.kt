package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires du parseur CSV BRED.
 * Aucune dépendance Android — le parseur est un objet Kotlin pur.
 *
 * Format testé : séparateur point-virgule, dates DD/MM/YYYY, montants notation française.
 */
class BredCsvParserTest {

    // ─── Constructeurs CSV ────────────────────────────────────────────────────

    /** Format A : Date;Libellé;Montant;Devise */
    private fun csvFormatA(vararg lignes: String): ByteArray =
        (listOf("Date;Libellé;Montant;Devise") + lignes.toList())
            .joinToString("\n")
            .toByteArray(Charsets.UTF_8)

    /** Format B : Date opération;Date valeur;Libellé;Montant;Devise */
    private fun csvFormatB(vararg lignes: String): ByteArray =
        (listOf("Date opération;Date valeur;Libellé;Montant;Devise") + lignes.toList())
            .joinToString("\n")
            .toByteArray(Charsets.UTF_8)

    // ─── Cas limites ──────────────────────────────────────────────────────────

    @Test
    fun `CSV vide retourne liste vide`() {
        val result = BredCsvParser.parse("".byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `en-tête seul sans données retourne liste vide`() {
        val result = BredCsvParser.parse(csvFormatA().inputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ligne avec montant zéro est ignorée`() {
        val result = BredCsvParser.parse(csvFormatA("01/05/2026;VIREMENT TEST;0,00;EUR").inputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ligne avec trop peu de colonnes est ignorée`() {
        val result = BredCsvParser.parse(csvFormatA("01/05/2026;LIBELLE SEUL").inputStream())
        assertTrue(result.isEmpty())
    }

    // ─── Format A — parsing de base ──────────────────────────────────────────

    @Test
    fun `dépense carte est parsée correctement (format A)`() {
        val result = BredCsvParser.parse(
            csvFormatA("15/04/2026;PAIEMENT CB 14/04 CARREFOUR PARIS;-52,80;EUR").inputStream()
        )

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate.of(2026, 4, 15), tx.date)
        assertEquals(5280L, tx.amountCents)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Currency.EUR, tx.currency)
        assertEquals("PAIEMENT CB 14/04 CARREFOUR PARIS", tx.note)
        assertEquals("bred", tx.importSource)
    }

    @Test
    fun `revenu virement est parsé avec type INCOME (format A)`() {
        val result = BredCsvParser.parse(
            csvFormatA("30/04/2026;VIR SEPA RECU SALAIRE ENTREPRISE;3500,00;EUR").inputStream()
        )

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(350000L, tx.amountCents)
        assertEquals(Category.AUTRE, tx.category)
    }

    // ─── Format B — 5 colonnes avec date valeur ──────────────────────────────

    @Test
    fun `format B avec date valeur est correctement parsé`() {
        val result = BredCsvParser.parse(
            csvFormatB("15/04/2026;17/04/2026;PRLV SEPA EDF ENERGIE;-120,50;EUR").inputStream()
        )

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate.of(2026, 4, 15), tx.date)
        assertEquals(12050L, tx.amountCents)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("PRLV SEPA EDF ENERGIE", tx.note)
    }

    // ─── Catégorisation automatique ──────────────────────────────────────────

    @Test
    fun `paiement LECLERC est catégorisé ALIMENTATION`() {
        val result = BredCsvParser.parse(
            csvFormatA("10/05/2026;PAIEMENT CB LECLERC BORDEAUX;-65,30;EUR").inputStream()
        )
        assertEquals(Category.ALIMENTATION, result[0].category)
    }

    @Test
    fun `prélèvement EDF est catégorisé ABONNEMENTS`() {
        val result = BredCsvParser.parse(
            csvFormatA("05/05/2026;PRLV SEPA EDF ENERGIE;-120,00;EUR").inputStream()
        )
        assertEquals(Category.ABONNEMENTS, result[0].category)
    }

    @Test
    fun `virement sortant est catégorisé TRANSFERTS`() {
        val result = BredCsvParser.parse(
            csvFormatA("12/05/2026;VIR SEPA EMIS FAMILLE;-200,00;EUR").inputStream()
        )
        assertEquals(Category.TRANSFERTS, result[0].category)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `retrait DAB est catégorisé AUTRE`() {
        val result = BredCsvParser.parse(
            csvFormatA("08/05/2026;RETRAIT DAB PARIS 11;-100,00;EUR").inputStream()
        )
        assertEquals(Category.AUTRE, result[0].category)
    }

    @Test
    fun `libellé inconnu est catégorisé AUTRE`() {
        val result = BredCsvParser.parse(
            csvFormatA("20/05/2026;DIVERS OPERATION SPECIALE;-15,99;EUR").inputStream()
        )
        assertEquals(Category.AUTRE, result[0].category)
    }

    // ─── Formats de montants français ─────────────────────────────────────────

    @Test
    fun `montant avec espace milliers est correctement parsé`() {
        val result = BredCsvParser.parse(
            csvFormatA("01/05/2026;VIR RECU PRIME;1 234,56;EUR").inputStream()
        )
        assertEquals(123456L, result[0].amountCents)
    }

    @Test
    fun `tiret long unicode dans les montants est géré`() {
        val montantAvecTiretLong = "−45,99" // U+2212
        val result = BredCsvParser.parse(
            csvFormatA("01/05/2026;ACHAT DIVERS;$montantAvecTiretLong;EUR").inputStream()
        )
        assertEquals(1, result.size)
        assertEquals(4599L, result[0].amountCents)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    // ─── Déduplication ────────────────────────────────────────────────────────

    @Test
    fun `deux transactions identiques le même jour ont des externalIds identiques`() {
        val ligne = "01/05/2026;PAIEMENT CB MONOPRIX;-25,00;EUR"
        val r1 = BredCsvParser.parse(csvFormatA(ligne).inputStream())
        val r2 = BredCsvParser.parse(csvFormatA(ligne).inputStream())
        assertEquals(r1[0].externalId, r2[0].externalId)
    }

    @Test
    fun `deux transactions différentes ont des externalIds différents`() {
        val result = BredCsvParser.parse(
            csvFormatA(
                "01/05/2026;PAIEMENT CB MONOPRIX;-25,00;EUR",
                "02/05/2026;PAIEMENT CB FNAC;-49,90;EUR"
            ).inputStream()
        )
        assertEquals(2, result.size)
        assertNotEquals(result[0].externalId, result[1].externalId)
    }

    // ─── Plusieurs lignes ─────────────────────────────────────────────────────

    @Test
    fun `plusieurs lignes valides sont toutes parsées`() {
        val result = BredCsvParser.parse(
            csvFormatA(
                "01/05/2026;PAIEMENT CB CARREFOUR;-45,00;EUR",
                "02/05/2026;VIR SEPA RECU LOYER;800,00;EUR",
                "03/05/2026;PRLV SEPA SFR;-29,99;EUR"
            ).inputStream()
        )
        assertEquals(3, result.size)
    }
}
