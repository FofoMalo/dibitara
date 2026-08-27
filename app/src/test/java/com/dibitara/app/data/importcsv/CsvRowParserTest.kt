package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.ModeMontant
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [CsvRowParser]. Objet Kotlin pur.
 */
class CsvRowParserTest {

    private val mappingSigne = CsvColumnMapping(
        delimiteur = ';',
        aEnTete = true,
        colonneDate = 0,
        formatDate = "dd/MM/yyyy",
        modeMontant = ModeMontant.COLONNE_SIGNEE,
        colonneMontant = 2,
        separateurDecimal = ',',
        colonnesLibelle = listOf(1),
    )

    private val mappingDebitCredit = CsvColumnMapping(
        delimiteur = ';',
        aEnTete = true,
        colonneDate = 0,
        formatDate = "dd/MM/yyyy",
        modeMontant = ModeMontant.DEBIT_CREDIT,
        colonneDebit = 2,
        colonneCredit = 3,
        separateurDecimal = ',',
        colonnesLibelle = listOf(1),
    )

    // ─── Colonne montant signée ──────────────────────────────────────────────

    @Test
    fun `montant négatif devient une dépense`() {
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Courses", "-42,90")),
            mappingSigne, Currency.EUR
        )
        val t = res.transactions.single()
        assertEquals(TransactionType.EXPENSE, t.type)
        assertEquals(4290L, t.amountCents)
        assertEquals("Courses", t.note)
        assertEquals(Currency.EUR, t.currency)
    }

    @Test
    fun `montant positif devient un revenu`() {
        val res = CsvRowParser.parser(
            listOf(listOf("03/02/2026", "Salaire", "2500,00")),
            mappingSigne, Currency.EUR
        )
        assertEquals(TransactionType.INCOME, res.transactions.single().type)
        assertEquals(250_000L, res.transactions.single().amountCents)
    }

    @Test
    fun `la conversion en centimes évite la troncature IEEE754`() {
        // 35,30 * 100 vaut 3529,9999... en binaire : roundToLong doit ramener à 3530
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Truc", "-35,30")),
            mappingSigne, Currency.EUR
        )
        assertEquals(3530L, res.transactions.single().amountCents)
    }

    // ─── Colonnes débit / crédit ─────────────────────────────────────────────

    @Test
    fun `ligne avec débit renseigné est une dépense`() {
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Achat", "42,90", "")),
            mappingDebitCredit, Currency.EUR
        )
        assertEquals(TransactionType.EXPENSE, res.transactions.single().type)
        assertEquals(4290L, res.transactions.single().amountCents)
    }

    @Test
    fun `ligne avec crédit renseigné est un revenu`() {
        val res = CsvRowParser.parser(
            listOf(listOf("03/02/2026", "Salaire", "", "2500,00")),
            mappingDebitCredit, Currency.EUR
        )
        assertEquals(TransactionType.INCOME, res.transactions.single().type)
    }

    // ─── Lignes ignorées ─────────────────────────────────────────────────────

    @Test
    fun `ligne à la date illisible est ignorée`() {
        val res = CsvRowParser.parser(
            listOf(
                listOf("pas une date", "X", "-10,00"),
                listOf("01/02/2026", "OK", "-10,00"),
            ),
            mappingSigne, Currency.EUR
        )
        assertEquals(1, res.transactions.size)
        assertEquals(1, res.lignesIgnorees)
    }

    @Test
    fun `ligne au montant nul est ignorée`() {
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Régularisation", "0,00")),
            mappingSigne, Currency.EUR
        )
        assertEquals(0, res.transactions.size)
        assertEquals(1, res.lignesIgnorees)
    }

    // ─── Libellé multi-colonnes et devise ────────────────────────────────────

    @Test
    fun `plusieurs colonnes de libellé sont concaténées`() {
        val mapping = mappingSigne.copy(colonnesLibelle = listOf(1, 3))
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "CARTE", "-12,00", "BOULANGERIE PARIS")),
            mapping, Currency.EUR
        )
        assertEquals("CARTE BOULANGERIE PARIS", res.transactions.single().note)
    }

    @Test
    fun `la devise est lue depuis sa colonne quand elle est mappée`() {
        val mapping = mappingSigne.copy(colonneDevise = 3)
        val res = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Achat USD", "-12,00", "USD")),
            mapping, Currency.EUR
        )
        assertEquals(Currency.USD, res.transactions.single().currency)
    }

    // ─── externalId ──────────────────────────────────────────────────────────

    @Test
    fun `externalId identique pour la même transaction, différent sinon`() {
        fun parse(montant: String) = CsvRowParser.parser(
            listOf(listOf("01/02/2026", "Courses", montant)), mappingSigne, Currency.EUR
        ).transactions.single().externalId

        assertEquals(parse("-42,90"), parse("-42,90"))
        assertNotEquals(parse("-42,90"), parse("-43,90"))
    }
}
