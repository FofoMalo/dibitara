package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires du parseur de notifications push BRED.
 * Textes réels capturés par Florent le 03/08/2026 (voir mémoire du projet).
 */
class BredNotificationParserTest {

    @Test
    fun `paiement carte est parsé correctement`() {
        val texte = "La BRED vous confirme votre paiement carte d'un montant de 35,30€ " +
            "(BAR LES ARCADES) le 03/08/2026. Pour plus d'informations contactez Bred Direct " +
            "au 0806 060 211 (service gratuit + prix appel)."

        val tx = BredNotificationParser.parse(texte)

        assertNotNull(tx)
        assertEquals(LocalDate.of(2026, 8, 3), tx!!.date)
        assertEquals(3530L, tx.amountCents)
        assertEquals(Currency.EUR, tx.currency)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("BAR LES ARCADES", tx.note)
        assertEquals("bred_notification", tx.importSource)
        assertEquals(Category.ALIMENTATION, tx.category)
    }

    @Test
    fun `deux notifications identiques génèrent le même externalId`() {
        val texte = "La BRED vous confirme votre paiement carte d'un montant de 35,30€ " +
            "(BAR LES ARCADES) le 03/08/2026."

        val tx1 = BredNotificationParser.parse(texte)
        val tx2 = BredNotificationParser.parse(texte)

        assertEquals(tx1!!.externalId, tx2!!.externalId)
    }

    @Test
    fun `notification Trade Republic n'est pas reconnue`() {
        val texte = "Dépensé 5,60 € à CEALVI"

        val tx = BredNotificationParser.parse(texte)

        assertNull(tx)
    }

    @Test
    fun `texte vide retourne null`() {
        assertNull(BredNotificationParser.parse(""))
    }

    @Test
    fun `notification sans montant reconnaissable retourne null`() {
        val texte = "La BRED vous informe d'une mise à jour de vos conditions générales."

        assertNull(BredNotificationParser.parse(texte))
    }
}
