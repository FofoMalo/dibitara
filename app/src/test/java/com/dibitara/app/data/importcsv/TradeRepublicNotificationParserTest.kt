package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires du parseur de notifications push TradeRepublic.
 * Texte réel capturé par Florent le 07/09/2026 (voir mémoire du projet).
 */
class TradeRepublicNotificationParserTest {

    @Test
    fun `paiement carte est parsé correctement`() {
        val texte = "Dépensé 28,45 € à FRANPRIX"

        val tx = TradeRepublicNotificationParser.parse(texte)

        assertNotNull(tx)
        assertEquals(LocalDate.now(), tx!!.date)
        assertEquals(2845L, tx.amountCents)
        assertEquals(Currency.EUR, tx.currency)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("FRANPRIX", tx.note)
        assertEquals("trade_republic_notification", tx.importSource)
        assertEquals(Category.ALIMENTATION, tx.category)
    }

    @Test
    fun `marchand sans mot-clé connu est catégorisé AUTRE`() {
        val texte = "Dépensé 13,50 € à MR LEMAITRE BENA"

        val tx = TradeRepublicNotificationParser.parse(texte)

        assertNotNull(tx)
        assertEquals(1350L, tx!!.amountCents)
        assertEquals("MR LEMAITRE BENA", tx.note)
        assertEquals(Category.AUTRE, tx.category)
    }

    @Test
    fun `deux paiements du même montant le même jour à des marchands différents ont un externalId distinct`() {
        // Contrairement aux captures BRED (genererExternalIdMontantDate), le marchand fait partie
        // de la clé ici : les paiements carte TradeRepublic sont le flux de dépense quotidien, pas
        // un cas rare - une clé sans marchand ferait passer le second paiement pour un doublon.
        val tx1 = TradeRepublicNotificationParser.parse("Dépensé 13,50 € à FRANPRIX")
        val tx2 = TradeRepublicNotificationParser.parse("Dépensé 13,50 € à MR LEMAITRE BENA")

        assertNotEquals(tx1!!.externalId, tx2!!.externalId)
    }

    @Test
    fun `deux notifications identiques génèrent le même externalId`() {
        val texte = "Dépensé 28,45 € à FRANPRIX"

        val tx1 = TradeRepublicNotificationParser.parse(texte)
        val tx2 = TradeRepublicNotificationParser.parse(texte)

        assertEquals(tx1!!.externalId, tx2!!.externalId)
    }

    @Test
    fun `notification BRED n'est pas reconnue`() {
        val texte = "La BRED vous confirme votre paiement carte d'un montant de 35,30€ " +
            "(BAR LES ARCADES) le 03/08/2026."

        assertNull(TradeRepublicNotificationParser.parse(texte))
    }

    @Test
    fun `texte vide retourne null`() {
        assertNull(TradeRepublicNotificationParser.parse(""))
    }

    @Test
    fun `notification sans montant reconnaissable retourne null`() {
        val texte = "Votre relevé mensuel TradeRepublic est disponible."

        assertNull(TradeRepublicNotificationParser.parse(texte))
    }
}
