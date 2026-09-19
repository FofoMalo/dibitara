package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TradeRepublicInvestmentNotificationTest {
    private val date = LocalDate.of(2026, 9, 9)
    private val plan = "Votre plan d’épargne sur Core MSCI World USD (Acc) de 35,00 € a été exécuté"
    private val roundup = "Vous avez économisé et investi 48,66 € dans le Round up !"

    @Test
    fun `plan de la capture réelle conserve support montant et date`() {
        val tx = TradeRepublicNotificationParser.parse(plan, date, "plan-1")!!
        assertEquals(3500L, tx.amountCents)
        assertEquals(date, tx.date)
        assertEquals(Currency.EUR, tx.currency)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Category.INVESTISSEMENT, tx.category)
        assertEquals("Plan d’épargne · Core MSCI World USD (Acc)", tx.note)
        assertEquals("SAVINGS_PLAN_NOTIF", tx.rawType)
        assertEquals("trade_republic_notification", tx.importSource)
    }

    @Test
    fun `roundup de la capture ne suppose pas de support`() {
        val tx = TradeRepublicNotificationParser.parse(roundup, date, "roundup-1")!!
        assertEquals(4866L, tx.amountCents)
        assertEquals(Category.INVESTISSEMENT, tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Roundup investi", tx.note)
        assertEquals("ROUNDUP_NOTIF", tx.rawType)
    }

    @Test
    fun `retours ligne apostrophe droite et espaces monétaires sont tolérés`() {
        val texte = plan.replace('’', '\'').replace("Core MSCI World", "Core MSCI\nWorld")
            .replace("35,00 €", "1\u202F235,09\u00A0€")
        assertEquals(123509L, TradeRepublicNotificationParser.parse(texte, date)!!.amountCents)
    }

    @Test
    fun `échec annonce montant invalide et message tiers sont ignorés`() {
        listOf(
            plan.replace("a été exécuté", "sera exécuté"),
            plan.replace("a été exécuté", "n’a pas été exécuté"),
            plan.replace("35,00", "0,00"),
            plan.replace("35,00", "35,001"),
            plan.replace("35,00", "-35,00"),
            "Virement reçu de 35,00 €", "Round up investi", ""
        ).forEach { assertNull(TradeRepublicNotificationParser.parse(it, date), it) }
    }

    @Test
    fun `rejeu stable et exécutions distinctes de même montant`() {
        val premier = TradeRepublicNotificationParser.parse(plan, date, "id-1")!!
        assertEquals(premier.externalId, TradeRepublicNotificationParser.parse(plan, date, "id-1")!!.externalId)
        assertNotEquals(premier.externalId, TradeRepublicNotificationParser.parse(plan, date, "id-2")!!.externalId)
        assertNotEquals(premier.externalId, TradeRepublicNotificationParser.parse(roundup.replace("48,66", "35,00"), date, "id-1")!!.externalId)
    }
}
