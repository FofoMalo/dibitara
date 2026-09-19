package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class RapprocherReleveUseCaseTest {
    private val date = LocalDate.of(2026, 9, 2)
    private val account = BankAccount(1, BankProvider.BRED, "Compte", 0, Currency.EUR, date)
    private fun tx(id: Long, amount: Long, type: TransactionType = TransactionType.EXPENSE) =
        Transaction(id, amount, Currency.EUR, Category.TRANSFERTS, type, date, bankAccountId = 1)
    @Test fun `le rapprochement garde les virements et respecte le compte et le mois`() {
        val result = RapprocherReleveUseCase()(account, YearMonth.of(2026, 9), 10000, 11500,
            listOf(tx(1, 3000, TransactionType.INCOME), tx(2, 1500), tx(3, 999).copy(bankAccountId = 2), tx(4, 888).copy(date = date.minusMonths(1))))
        assertEquals(11500L, result.soldeCalculeCents)
        assertEquals(0L, result.ecartCents)
        assertEquals(2, result.nombreOperations)
    }
    @Test fun `les devises non comparables et investissements sont signales`() {
        val result = RapprocherReleveUseCase()(account, YearMonth.of(2026, 9), 10000, 8000,
            listOf(tx(1, 2000, TransactionType.INVESTMENT), tx(2, 1000).copy(currency = Currency.CAD)))
        assertEquals(8000L, result.soldeCalculeCents)
        assertEquals(1, result.devisesNonComparees)
        assertEquals(1, result.investissementsAVerifier)
    }
    @Test fun `un ecart negatif indique un solde releve inferieur`() {
        assertEquals(-500L, RapprocherReleveUseCase()(account, YearMonth.of(2026, 9), 1000, 500, emptyList()).ecartCents)
    }
}
