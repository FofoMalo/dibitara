package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.ModeMontant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [CsvColumnDetector].
 */
class CsvColumnDetectorTest {

    private fun rows(vararg lignes: List<String>) = lignes.toList()

    // ─── CSV bien formé, notation FR ──────────────────────────────────────────

    @Test
    fun `relevé FR avec en-tête - mapping complet deviné`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date", "Libellé", "Montant", "Devise"),
                listOf("01/02/2026", "PAIEMENT CB BOULANGERIE", "-4,20", "EUR"),
                listOf("03/02/2026", "VIREMENT SALAIRE", "2500,00", "EUR"),
                listOf("05/02/2026", "PRLV EDF ENERGIE", "-89,90", "EUR"),
            ),
            delimiteur = ';'
        )

        assertTrue(mapping.aEnTete)
        assertEquals(0, mapping.colonneDate)
        assertEquals("dd/MM/yyyy", mapping.formatDate)
        assertEquals(ModeMontant.COLONNE_SIGNEE, mapping.modeMontant)
        assertEquals(2, mapping.colonneMontant)
        assertEquals(',', mapping.separateurDecimal)
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertEquals(3, mapping.colonneDevise)
        assertTrue(mapping.estComplet)
    }

    // ─── CSV EN, date ISO, séparateur décimal point ──────────────────────────

    @Test
    fun `relevé EN avec date ISO et point décimal`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date", "Description", "Amount"),
                listOf("2026-02-01", "Groceries", "-42.90"),
                listOf("2026-02-03", "Salary", "2500.00"),
            ),
            delimiteur = ','
        )

        assertEquals(0, mapping.colonneDate)
        assertEquals("yyyy-MM-dd", mapping.formatDate)
        assertEquals('.', mapping.separateurDecimal)
        assertEquals(2, mapping.colonneMontant)
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertTrue(mapping.estComplet)
    }

    // ─── Colonnes Débit / Crédit séparées ────────────────────────────────────

    @Test
    fun `colonnes débit et crédit distinctes - mode DEBIT_CREDIT`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date opération", "Nature", "Débit", "Crédit"),
                listOf("01/02/2026", "ACHAT MAGASIN", "42,90", ""),
                listOf("03/02/2026", "SALAIRE FEVRIER", "", "2500,00"),
            ),
            delimiteur = ';'
        )

        assertEquals(ModeMontant.DEBIT_CREDIT, mapping.modeMontant)
        assertEquals(2, mapping.colonneDebit)
        assertEquals(3, mapping.colonneCredit)
        assertEquals(null, mapping.colonneMontant)
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertTrue(mapping.estComplet)
    }

    // ─── Sans ligne d'en-tête ────────────────────────────────────────────────

    @Test
    fun `fichier sans en-tête - détection par le contenu`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("01/02/2026", "Boulangerie du centre", "-4,20"),
                listOf("03/02/2026", "Salaire", "2500,00"),
                listOf("05/02/2026", "EDF prelevement mensuel", "-89,90"),
            ),
            delimiteur = ';'
        )

        assertFalse(mapping.aEnTete)
        assertEquals(0, mapping.colonneDate)
        assertEquals(2, mapping.colonneMontant)
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertTrue(mapping.estComplet)
    }

    // ─── Date américaine ─────────────────────────────────────────────────────

    @Test
    fun `date au format américain MM slash dd slash yyyy`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date", "Memo", "Amount"),
                listOf("02/15/2026", "Rent", "-1200.00"),
                listOf("02/20/2026", "Paycheck", "3000.00"),
            ),
            delimiteur = ','
        )

        assertEquals("MM/dd/yyyy", mapping.formatDate)
        assertTrue(mapping.estComplet)
    }

    // ─── Cas incomplets → écran de mapping requis ────────────────────────────

    @Test
    fun `échantillon vide - mapping incomplet`() {
        val mapping = CsvColumnDetector.detecter(emptyList(), ';')
        assertEquals(-1, mapping.colonneDate)
        assertFalse(mapping.estComplet)
    }

    @Test
    fun `colonne montant absente - mapping incomplet`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date", "Libellé"),
                listOf("01/02/2026", "Boulangerie"),
                listOf("03/02/2026", "Pharmacie"),
            ),
            delimiteur = ';'
        )
        assertEquals(0, mapping.colonneDate)
        assertEquals(null, mapping.colonneMontant)
        assertFalse(mapping.estComplet)
    }

    // ─── Synonymes d'en-tête ─────────────────────────────────────────────────

    // ─── Colonne « Référence » exclue du libellé ─────────────────────────────

    @Test
    fun `relevé BCEAO - la colonne Référence ne pollue pas le libellé`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Date", "Libellé", "Montant_Débit_FCFA", "Montant_Crédit_FCFA",
                    "Solde_FCFA", "Référence", "Catégorie"),
                listOf("2026-08-01", "Retrait DAB", "50000", "", "500000", "DAB-20260801-001", "Retrait"),
                listOf("2026-08-02", "Virement reçu - Client A", "", "200000", "700000",
                    "VIR-20260802-002", "Virement"),
            ),
            delimiteur = ';'
        )

        assertEquals(ModeMontant.DEBIT_CREDIT, mapping.modeMontant)
        assertEquals(2, mapping.colonneDebit)
        assertEquals(3, mapping.colonneCredit)
        // Seule la colonne « Libellé » : ni « Référence » (id technique) ni « Catégorie ».
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertTrue(mapping.estComplet)
    }

    @Test
    fun `reconnaît des en-têtes synonymes`() {
        val mapping = CsvColumnDetector.detecter(
            rows(
                listOf("Value date", "Transaction", "Valeur", "Currency"),
                listOf("2026-02-01", "Achat carte", "-30,00", "EUR"),
                listOf("2026-02-02", "Remboursement", "12,50", "EUR"),
            ),
            delimiteur = ';'
        )
        assertEquals(0, mapping.colonneDate)
        assertEquals(2, mapping.colonneMontant)
        assertEquals(listOf(1), mapping.colonnesLibelle)
        assertEquals(3, mapping.colonneDevise)
    }
}
