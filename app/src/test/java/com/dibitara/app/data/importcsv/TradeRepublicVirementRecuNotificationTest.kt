package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Textes réels capturés dans trade_republic_notif_debug.log (2026-09-16 et 2026-09-18) :
 * - "Tu as reçu 200,00 € de EI-MALO NOUMEHAN\nVirement reçu"
 * - "Tu as reçu 300,00 € de MR   ET MME NOUMEHAN MALO\nVirement reçu"
 * Non reconnus jusqu'ici faute d'exemple réel confirmé (voir TradeRepublicNotificationParser).
 */
class TradeRepublicVirementRecuNotificationTest {
    private val date = LocalDate.of(2026, 9, 16)

    @Test
    fun `virement reçu de la capture réelle est catégorisé comme un revenu tiers`() {
        val texte = "Tu as reçu 200,00 € de EI-MALO NOUMEHAN\nVirement reçu"

        val tx = TradeRepublicNotificationParser.parse(texte, date)

        assertNotNull(tx)
        assertEquals(20_000L, tx!!.amountCents)
        assertEquals(Currency.EUR, tx.currency)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(Category.AUTRE, tx.category)
        assertEquals("EI-MALO NOUMEHAN", tx.note)
        assertEquals("CUSTOMER_INBOUND_NOTIF", tx.rawType)
        assertEquals("trade_republic_notification", tx.importSource)
    }

    @Test
    fun `nom d'expéditeur avec espaces multiples est conservé tel quel`() {
        val texte = "Tu as reçu 300,00 € de MR   ET MME NOUMEHAN MALO\nVirement reçu"

        val tx = TradeRepublicNotificationParser.parse(texte, date)

        assertEquals(30_000L, tx!!.amountCents)
        assertEquals("MR   ET MME NOUMEHAN MALO", tx.note)
    }

    @Test
    fun `champ isolé sans la ligne Virement reçu est aussi reconnu`() {
        // Le service Android analyse title/text/bigText séparément (voir
        // TradeRepublicNotificationListenerService) - un seul de ces champs peut ne contenir
        // que la première ligne.
        val tx = TradeRepublicNotificationParser.parse("Tu as reçu 200,00 € de EI-MALO NOUMEHAN", date)

        assertEquals(20_000L, tx!!.amountCents)
        assertEquals("EI-MALO NOUMEHAN", tx.note)
    }

    @Test
    fun `deux virements du même montant le même jour d'expéditeurs différents ont un externalId distinct`() {
        val tx1 = TradeRepublicNotificationParser.parse("Tu as reçu 200,00 € de EI-MALO NOUMEHAN", date)
        val tx2 = TradeRepublicNotificationParser.parse("Tu as reçu 200,00 € de MR ET MME NOUMEHAN MALO", date)

        assertNotEquals(tx1!!.externalId, tx2!!.externalId)
    }

    @Test
    fun `reconciliationKey distingue un virement d'un paiement carte du même nom`() {
        val virement = TradeRepublicNotificationParser.parse("Tu as reçu 50,00 € de FRANPRIX", date)
        val paiementCarte = TradeRepublicNotificationParser.parse("Dépensé 50,00 € à FRANPRIX", date)

        assertNotEquals(virement!!.reconciliationKey, paiementCarte!!.reconciliationKey)
    }
}
