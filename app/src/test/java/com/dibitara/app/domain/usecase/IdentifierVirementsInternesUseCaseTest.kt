package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IdentifierVirementsInternesUseCaseTest {

    private val useCase = IdentifierVirementsInternesUseCase()

    private fun sortantBred(id: Long, amountCents: Long, date: LocalDate, currency: Currency = Currency.EUR) =
        Transaction(
            id = id, amountCents = amountCents, currency = currency,
            category = Category.TRANSFERTS, type = TransactionType.EXPENSE,
            date = date, importSource = "bred_csv"
        )

    private fun entrantTradeRepublic(id: Long, amountCents: Long, date: LocalDate, currency: Currency = Currency.EUR) =
        Transaction(
            id = id, amountCents = amountCents, currency = currency,
            category = Category.TRANSFERTS, type = TransactionType.INCOME,
            date = date, importSource = "trade_republic"
        )

    @Test
    fun `aucune transaction ne retourne un ensemble vide`() {
        assertTrue(useCase(emptyList()).isEmpty())
    }

    @Test
    fun `apparie un virement sortant BRED et un virement entrant TradeRepublic de même montant`() {
        val sortant = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1))
        val entrant = entrantTradeRepublic(2, 50_000L, LocalDate.of(2026, 8, 2))

        val resultat = useCase(listOf(sortant, entrant))

        assertEquals(setOf(1L, 2L), resultat)
    }

    @Test
    fun `montants différents ne sont pas appariés`() {
        val sortant = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1))
        val entrant = entrantTradeRepublic(2, 40_000L, LocalDate.of(2026, 8, 2))

        val resultat = useCase(listOf(sortant, entrant))

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `dates trop éloignées ne sont pas appariées`() {
        val sortant = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1))
        val entrant = entrantTradeRepublic(2, 50_000L, LocalDate.of(2026, 8, 10))

        val resultat = useCase(listOf(sortant, entrant))

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `un virement entrant sans virement sortant correspondant reste non apparié`() {
        // Ex. dépôt externe reçu sur TradeRepublic, pas un virement depuis BRED
        val entrant = entrantTradeRepublic(1, 50_000L, LocalDate.of(2026, 8, 1))

        val resultat = useCase(listOf(entrant))

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `un virement TRANSFERTS vers un tiers externe (sans pendant TradeRepublic) reste non apparié`() {
        val sortant = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1))

        val resultat = useCase(listOf(sortant))

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `chaque transaction n'est appariée qu'une seule fois`() {
        val sortant1 = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1))
        val sortant2 = sortantBred(2, 50_000L, LocalDate.of(2026, 8, 1))
        val entrant  = entrantTradeRepublic(3, 50_000L, LocalDate.of(2026, 8, 2))

        val resultat = useCase(listOf(sortant1, sortant2, entrant))

        // Un seul des deux sortants trouve une correspondance, l'autre reste non apparié
        assertEquals(2, resultat.size)
        assertTrue(3L in resultat)
    }

    @Test
    fun `devises différentes ne sont pas appariées même à montant identique`() {
        val sortant = sortantBred(1, 50_000L, LocalDate.of(2026, 8, 1), Currency.EUR)
        val entrant = entrantTradeRepublic(2, 50_000L, LocalDate.of(2026, 8, 1), Currency.USD)

        val resultat = useCase(listOf(sortant, entrant))

        assertTrue(resultat.isEmpty())
    }
}
