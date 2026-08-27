package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour [BredPdfParser].
 * Utilise du texte synthétique reproduisant la sortie pdfbox d'un relevé BRED réel
 * (compte 518.06.4209, relevé n°5, mai 2026).
 */
class BredPdfParserTest {

    // ─── Texte synthétique ────────────────────────────────────────────────────

    private val textePdfSynthetique = """
        4 mai 2026
        Votre compte : 518.06.4209
        Relevé n°5

        Situation de vos comptes

        Compte Plafond Solde au 04/05/26
        Poste principal 5.913,04
        LDD Solidaire 12.000,00 10.997,45
        Livret Fidelis 75.000,00 22.015,10
        Livret A 22.950,00 14.653,17

        Relevé d'opérations du poste principal

        03.04 Solde précédent 6 720,70
        07.04 Carte 4217162 48,20 07.04.26
        decure fabien le 05/04/26 cb.xxxxx5463
        07.04 Prélèvement SEPA 2897174 69,99 07.04.26
        canal+ france
        07.04 Virement automatique 2739748 100,00 07.04.26
        mr malo noumehan ech 050426
        20.04 Virement instantané reçu 94AD670 377,00 20.04.26
        lydia solutions
        20.04 Retrait d'espèces à un DAB 0020988 60,00 20.04.26
        bred rouen st marc2
        07.04 Prélèvement echéance 005 de votre 2.879,25 07.04.26
        pret personnel habitat
    """.trimIndent()

    // ─── Extraction de l'année ────────────────────────────────────────────────

    @Test
    fun `annee extraite depuis l en-tete du relevé`() {
        val result = BredPdfParser.parseTexte(textePdfSynthetique)
        assertEquals(2026, result.annee)
    }

    // ─── Soldes ───────────────────────────────────────────────────────────────

    @Test
    fun `soldes extraits depuis section Situation de vos comptes`() {
        val soldes = BredPdfParser.parseTexte(textePdfSynthetique).soldes
        assertTrue(soldes.isNotEmpty(), "Des soldes doivent être extraits")
    }

    @Test
    fun `poste principal sans plafond`() {
        val soldes = BredPdfParser.parseTexte(textePdfSynthetique).soldes
        val poste = soldes.firstOrNull { it.label.contains("principal", ignoreCase = true) }
        assertNotNull(poste, "Poste principal attendu dans les soldes")
        assertNull(poste!!.plafondCents, "Poste principal n'a pas de plafond")
        assertEquals(591_304L, poste.soldeCents)
    }

    @Test
    fun `LDD Solidaire avec plafond 12000 euros`() {
        val soldes = BredPdfParser.parseTexte(textePdfSynthetique).soldes
        val ldd = soldes.firstOrNull { it.label.contains("LDD", ignoreCase = true) }
        assertNotNull(ldd, "LDD Solidaire attendu")
        assertEquals(1_200_000L, ldd!!.plafondCents)
        assertEquals(1_099_745L, ldd.soldeCents)
    }

    @Test
    fun `Livret A avec plafond 22950 euros`() {
        val soldes = BredPdfParser.parseTexte(textePdfSynthetique).soldes
        val livretA = soldes.firstOrNull { it.label.contains("Livret A", ignoreCase = true) }
        assertNotNull(livretA, "Livret A attendu")
        assertEquals(2_295_000L, livretA!!.plafondCents)
    }

    // ─── Transactions ─────────────────────────────────────────────────────────

    @Test
    fun `solde precedent est ignore`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        assertTrue(
            transactions.none { it.note.contains("Solde précédent", ignoreCase = true) },
            "Solde précédent ne doit pas être une transaction"
        )
    }

    @Test
    fun `paiement carte est une dépense`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        val carte = transactions.firstOrNull { it.rawType.contains("Carte", ignoreCase = true) }
        assertNotNull(carte, "Transaction Carte attendue")
        assertEquals(TransactionType.EXPENSE, carte!!.type)
        assertEquals(4_820L, carte.amountCents)
    }

    @Test
    fun `virement instantane recu est un revenu`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        val vir = transactions.firstOrNull { it.rawType.contains("Virement", ignoreCase = true) && it.type == TransactionType.INCOME }
        assertNotNull(vir, "Virement reçu attendu")
        assertEquals(37_700L, vir!!.amountCents)
    }

    @Test
    fun `prelevement SEPA categorise en abonnements`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        val prlv = transactions.firstOrNull { it.rawType.contains("Prélèvement SEPA", ignoreCase = true) }
        assertNotNull(prlv, "Prélèvement SEPA attendu")
        // Canal+ → ABONNEMENTS
        assertEquals(Category.ABONNEMENTS, prlv!!.category)
    }

    @Test
    fun `prelevement echeance categorise en logement`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        val ech = transactions.firstOrNull { it.rawType.contains("Prélèvement echéance", ignoreCase = true) }
        assertNotNull(ech, "Prélèvement échéance attendu")
        assertEquals(Category.LOGEMENT, ech!!.category)
        assertEquals(287_925L, ech.amountCents)
    }

    @Test
    fun `retrait DAB categorise en AUTRE`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        val dab = transactions.firstOrNull { it.rawType.contains("Retrait", ignoreCase = true) }
        assertNotNull(dab, "Retrait DAB attendu")
        assertEquals(Category.AUTRE, dab!!.category)
        assertEquals(6_000L, dab.amountCents)
    }

    @Test
    fun `externalId est prefixe bred`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        assertTrue(
            transactions.all { it.externalId.startsWith("bred_") },
            "Tous les externalIds doivent commencer par 'bred_'"
        )
    }

    @Test
    fun `importSource est bred`() {
        val transactions = BredPdfParser.parseTexte(textePdfSynthetique).transactions
        assertTrue(
            transactions.all { it.importSource == "bred" },
            "importSource doit être 'bred'"
        )
    }

    // ─── Texte vide ───────────────────────────────────────────────────────────

    @Test
    fun `texte vide retourne résultat vide`() {
        val result = BredPdfParser.parseTexte("")
        assertTrue(result.transactions.isEmpty())
        assertTrue(result.soldes.isEmpty())
    }

    @Test
    fun `texte sans section operations retourne transactions vides`() {
        val texte = "4 mai 2026\nSituation de vos comptes\nLivret A 22.950,00 14.653,17"
        val result = BredPdfParser.parseTexte(texte)
        assertTrue(result.transactions.isEmpty())
        assertEquals(2026, result.annee)
    }
}
