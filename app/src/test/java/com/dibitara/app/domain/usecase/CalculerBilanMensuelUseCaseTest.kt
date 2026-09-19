package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CalculerBilanMensuelUseCaseTest {
    @Test fun `totaux et categories partagent conversion et exclusion des virements`() {
        val date = LocalDate.of(2026, 9, 2)
        val sortie = Transaction(1, 10000, Currency.EUR, Category.TRANSFERTS, TransactionType.EXPENSE, date, importSource = "bred_csv")
        val entree = sortie.copy(id = 2, type = TransactionType.INCOME, importSource = "trade_republic")
        val achat = sortie.copy(id = 3, amountCents = 2000, currency = Currency.USD, category = Category.ALIMENTATION, importSource = null)
        val bilan = CalculerBilanMensuelUseCase()(listOf(sortie, entree, achat), Currency.EUR, ExchangeRates(2.0, 655.957, 0))
        assertEquals(1000L, bilan.depensesCents)
        assertEquals(0L, bilan.revenusCents)
        assertEquals(listOf(3L), bilan.transactions.map { it.id })
        assertEquals(Currency.EUR, bilan.transactions.single().currency)
        assertEquals(bilan.depensesCents, bilan.transactions.sumOf { it.amountCents })
    }
}
