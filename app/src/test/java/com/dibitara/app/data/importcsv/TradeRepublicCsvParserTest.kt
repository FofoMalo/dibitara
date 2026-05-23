package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires du parseur CSV TradeRepublic.
 * Aucune dépendance Android — le parseur est un objet Kotlin pur.
 *
 * Chaque ligne de test respecte les 23 colonnes du format TR :
 * datetime, date, account_type, category, type, asset_class, name, symbol,
 * shares, price, amount, fee, tax, currency, original_amount, original_currency,
 * fx_rate, description, transaction_id, counterparty_name, counterparty_iban,
 * payment_reference, mcc_code
 */
class TradeRepublicCsvParserTest {

    // ─── En-tête CSV TradeRepublic ─────────────────────────────────────────────
    private val entete = """"datetime","date","account_type","category","type","asset_class","name","symbol","shares","price","amount","fee","tax","currency","original_amount","original_currency","fx_rate","description","transaction_id","counterparty_name","counterparty_iban","payment_reference","mcc_code""""

    private fun csvStream(vararg lignes: String) =
        (listOf(entete) + lignes.toList())
            .joinToString("\n")
            .byteInputStream(Charsets.UTF_8)

    // ─── Cas limites ───────────────────────────────────────────────────────────

    @Test
    fun `CSV vide retourne liste vide`() {
        val result = TradeRepublicCsvParser.parse("".byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `entête seul sans données retourne liste vide`() {
        val result = TradeRepublicCsvParser.parse(csvStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ligne avec montant zéro et fee zéro est ignorée`() {
        val ligne = """"2025-01-01T00:00:00Z","2025-01-01","DEFAULT","CASH","CARD_TRANSACTION","","SHOP","","","","0.000000","","","EUR","","","","","uuid-zero","","","","5411""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ligne avec transaction_id vide est ignorée`() {
        val ligne = """"2025-01-01T00:00:00Z","2025-01-01","DEFAULT","CASH","CARD_TRANSACTION","","SHOP","","","","-10.000000","","","EUR","","","","","","","","","5411""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ligne avec moins de 23 colonnes est ignorée`() {
        val ligne = """"2025-01-01","DEFAULT","CASH","CARD_TRANSACTION""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))
        assertTrue(result.isEmpty())
    }

    // ─── Paiements carte (CARD_TRANSACTION) ───────────────────────────────────

    @Test
    fun `CARD_TRANSACTION MCC 5411 est catégorisé ALIMENTATION`() {
        val ligne = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","CARREFOUR","","","","-25.500000","","","EUR","","","","TR Card Transaction","uuid-001","","","","5411""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(Category.ALIMENTATION, tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(2550L, tx.amountCents)
        assertEquals("CARREFOUR", tx.note)
        assertEquals("uuid-001", tx.externalId)
        assertEquals(LocalDate.of(2025, 1, 14), tx.date)
        assertEquals(Currency.EUR, tx.currency)
    }

    @Test
    fun `CARD_TRANSACTION MCC 5812 (restaurant) est catégorisé ALIMENTATION`() {
        val ligne = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","BAR LE XV","","","","-14.000000","","","EUR","","","","TR Card Transaction","uuid-002","","","","5812""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(Category.ALIMENTATION, result[0].category)
    }

    @Test
    fun `CARD_TRANSACTION MCC 5942 (librairie) est catégorisé LOISIRS`() {
        val ligne = """"2025-01-15T12:32:36Z","2025-01-15","DEFAULT","CASH","CARD_TRANSACTION","","MR LEMAITRE BENA","","","","-13.000000","","","EUR","","","","TR Card Transaction","uuid-003","","","","5942""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(Category.LOISIRS, result[0].category)
    }

    @Test
    fun `CARD_TRANSACTION MCC inconnu est catégorisé AUTRE`() {
        val ligne = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","DIVERS SHOP","","","","-10.000000","","","EUR","","","","TR Card Transaction","uuid-004","","","","9999""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(Category.AUTRE, result[0].category)
    }

    @Test
    fun `CARD_TRANSACTION MCC absent est catégorisé AUTRE`() {
        val ligne = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","DIVERS SHOP","","","","-10.000000","","","EUR","","","","TR Card Transaction","uuid-005","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(Category.AUTRE, result[0].category)
    }

    // ─── Virements entrants ────────────────────────────────────────────────────

    @Test
    fun `CUSTOMER_INBOUND est INCOME avec la contrepartie comme note`() {
        val ligne = """"2024-10-30T20:07:44Z","2024-10-30","DEFAULT","CASH","CUSTOMER_INBOUND","","EI-MALO NOUMEHAN","","","","100.000000","","","EUR","","","","tradeRep","uuid-006","EI-MALO NOUMEHAN","FR761695800001","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(Category.AUTRE, tx.category)
        assertEquals(10000L, tx.amountCents)
        assertEquals("EI-MALO NOUMEHAN", tx.note)
    }

    @Test
    fun `TRANSFER_INBOUND est INCOME catégorie AUTRE`() {
        val ligne = """"2025-02-03T14:11:54Z","2025-02-03","DEFAULT","CASH","TRANSFER_INBOUND","","","","","","5.000000","","","EUR","","","","Incoming transfer from Florent MALO","uuid-007","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(Category.AUTRE, result[0].category)
        assertEquals(500L, result[0].amountCents)
    }

    @Test
    fun `TRANSFER_INSTANT_INBOUND est INCOME`() {
        val ligne = """"2025-03-04T04:13:10Z","2025-03-04","DEFAULT","CASH","TRANSFER_INSTANT_INBOUND","","","","","","100.000000","","","EUR","","","","Incoming transfer from EI-MALO","uuid-008","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(10000L, result[0].amountCents)
    }

    // ─── Virement sortant ──────────────────────────────────────────────────────

    @Test
    fun `TRANSFER_INSTANT_OUTBOUND est EXPENSE catégorie TRANSFERTS`() {
        val ligne = """"2025-02-08T00:17:23Z","2025-02-08","DEFAULT","CASH","TRANSFER_INSTANT_OUTBOUND","","","","","","-50.000000","","","EUR","","","","Outgoing transfer for Noumhan Malo","uuid-009","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        val tx = result[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Category.TRANSFERTS, tx.category)
        assertEquals(5000L, tx.amountCents)
    }

    // ─── Intérêts ─────────────────────────────────────────────────────────────

    @Test
    fun `INTEREST_PAYMENT est INCOME catégorie EPARGNE avec libellé fixe`() {
        val ligne = """"2024-12-01T17:53:51Z","2024-12-01","DEFAULT","CASH","INTEREST_PAYMENT","","","","","","0.170000","","0.00","EUR","","","","Interest payment Booking","uuid-010","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        val tx = result[0]
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(Category.EPARGNE, tx.category)
        assertEquals(17L, tx.amountCents)
        assertEquals("Intérêts TradeRepublic", tx.note)
    }

    // ─── Frais carte ──────────────────────────────────────────────────────────

    @Test
    fun `CARD_ORDERING_FEE utilise la colonne fee quand amount est zéro`() {
        val ligne = """"2024-10-31T08:22:25Z","2024-10-31","DEFAULT","CASH","CARD_ORDERING_FEE","","","","","","0.000000","-50.00","","EUR","","","","Trade Republic Card","uuid-011","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        val tx = result[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Category.AUTRE, tx.category)
        assertEquals(5000L, tx.amountCents)
        assertEquals("Frais carte TradeRepublic", tx.note)
    }

    // ─── Achat ETF (TRADING) ──────────────────────────────────────────────────

    @Test
    fun `BUY TRADING est EXPENSE catégorie INVESTISSEMENT avec le nom de l'ETF`() {
        val ligne = """"2025-03-10T15:11:08Z","2025-03-10","DEFAULT","TRADING","BUY","FUND","Core MSCI World USD (Acc)","IE000BI8OT95","0.0808170000","123.735000","-10.00","","","EUR","","","","Savings plan execution","uuid-012","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        val tx = result[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Category.INVESTISSEMENT, tx.category)
        assertEquals(1000L, tx.amountCents)
        assertEquals("Core MSCI World USD (Acc)", tx.note)
        assertEquals("BUY", tx.trRawType)
    }

    // ─── Conversion des montants ───────────────────────────────────────────────

    @Test
    fun `montant 123_735 euros est converti en 12373 centimes (troncature Long)`() {
        val ligne = """"2025-03-10T15:11:08Z","2025-03-10","DEFAULT","TRADING","BUY","FUND","ETF Test","SYM","0.1","123.735000","-123.735000","","","EUR","","","","","uuid-013","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne))

        // (123.735 * 100).toLong() = 12373 (troncature naturelle de toLong)
        assertEquals(12373L, result[0].amountCents)
    }

    // ─── Plusieurs lignes ─────────────────────────────────────────────────────

    @Test
    fun `plusieurs lignes valides sont toutes parsées`() {
        val ligne1 = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","CARREFOUR","","","","-25.500000","","","EUR","","","","TR Card Transaction","uuid-014","","","","5411""""
        val ligne2 = """"2025-03-10T15:11:08Z","2025-03-10","DEFAULT","TRADING","BUY","FUND","Core MSCI World","IE000","0.08","123.0","-10.00","","","EUR","","","","","uuid-015","","","",""""
        val result = TradeRepublicCsvParser.parse(csvStream(ligne1, ligne2))

        assertEquals(2, result.size)
    }

    @Test
    fun `lignes valides et invalides mélangées — seules les valides sont retournées`() {
        val valide   = """"2025-01-14T09:41:40Z","2025-01-14","DEFAULT","CASH","CARD_TRANSACTION","","SHOP","","","","-10.000000","","","EUR","","","","TR Card","uuid-016","","","","5411""""
        val invalide = """"2025-01-14","TROP_COURT""""
        val result = TradeRepublicCsvParser.parse(csvStream(valide, invalide))

        assertEquals(1, result.size)
        assertEquals("uuid-016", result[0].externalId)
    }
}
