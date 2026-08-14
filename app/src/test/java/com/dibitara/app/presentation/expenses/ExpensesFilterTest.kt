package com.dibitara.app.presentation.expenses

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests de [ExpensesFilter.apply].
 *
 * Le filtre de date est désormais géré au niveau SQL par le ViewModel selon la [FilterPeriod].
 * [apply] ne couvre que : catégorie, type, recherche textuelle et tri.
 */
class ExpensesFilterTest {

    private val today = LocalDate.of(2026, 5, 9)

    private fun buildExpense(
        note          : String           = "",
        category      : Category         = Category.ALIMENTATION,
        date          : LocalDate        = today,
        amountCents   : Long             = 1000L,
        type          : TransactionType  = TransactionType.EXPENSE,
        bankAccountId : Long?             = null
    ) = Transaction(
        id = 0, amountCents = amountCents, currency = Currency.EUR,
        category = category, type = type, date = date, note = note, bankAccountId = bankAccountId
    )

    @Test
    fun `filtre par note - retourne uniquement les correspondances`() {
        val list = listOf(buildExpense(note = "Courses Lidl"), buildExpense(note = "Loyer"))
        val result = ExpensesFilter(query = "courses").apply(list)
        assertEquals(1, result.size)
        assertEquals("Courses Lidl", result.first().note)
    }

    @Test
    fun `filtre par note est insensible à la casse`() {
        val list = listOf(buildExpense(note = "COURSES"), buildExpense(note = "loyer"))
        val result = ExpensesFilter(query = "courses").apply(list)
        assertEquals(1, result.size)
    }

    @Test
    fun `filtre par catégorie - retourne uniquement la catégorie sélectionnée`() {
        val list = listOf(
            buildExpense(category = Category.ALIMENTATION),
            buildExpense(category = Category.LOGEMENT)
        )
        val result = ExpensesFilter(category = Category.LOGEMENT).apply(list)
        assertEquals(1, result.size)
        assertEquals(Category.LOGEMENT, result.first().category)
    }

    @Test
    fun `filtre catégorie null retourne toutes les catégories`() {
        val list = listOf(
            buildExpense(category = Category.ALIMENTATION),
            buildExpense(category = Category.LOGEMENT)
        )
        val result = ExpensesFilter(category = null, transactionType = null).apply(list)
        assertEquals(2, result.size)
    }

    @Test
    fun `filtre type null retourne tous les types`() {
        val list = listOf(
            buildExpense(type = TransactionType.EXPENSE),
            buildExpense(type = TransactionType.INCOME)
        )
        val result = ExpensesFilter(transactionType = null).apply(list)
        assertEquals(2, result.size)
    }

    @Test
    fun `filtre type EXPENSE exclut les revenus`() {
        val list = listOf(
            buildExpense(type = TransactionType.EXPENSE),
            buildExpense(type = TransactionType.INCOME)
        )
        val result = ExpensesFilter(transactionType = TransactionType.EXPENSE).apply(list)
        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result.first().type)
    }

    @Test
    fun `filtre par compte bancaire - retourne uniquement les transactions du compte`() {
        val list = listOf(
            buildExpense(bankAccountId = 1L),
            buildExpense(bankAccountId = 2L),
            buildExpense(bankAccountId = null)
        )
        val result = ExpensesFilter(bankAccountId = 1L).apply(list)
        assertEquals(1, result.size)
        assertEquals(1L, result.first().bankAccountId)
    }

    @Test
    fun `filtre compte bancaire null retourne toutes les transactions`() {
        val list = listOf(buildExpense(bankAccountId = 1L), buildExpense(bankAccountId = null))
        val result = ExpensesFilter(bankAccountId = null).apply(list)
        assertEquals(2, result.size)
    }

    @Test
    fun `tri par montant décroissant`() {
        val list = listOf(
            buildExpense(amountCents = 500L),
            buildExpense(amountCents = 2000L),
            buildExpense(amountCents = 100L)
        )
        val result = ExpensesFilter(sort = SortOrder.AMOUNT_DESC).apply(list)
        assertEquals(2000L, result.first().amountCents)
        assertEquals(100L, result.last().amountCents)
    }

    @Test
    fun `tri par date décroissante`() {
        val list = listOf(
            buildExpense(date = today.minusDays(2)),
            buildExpense(date = today),
            buildExpense(date = today.minusDays(5))
        )
        val result = ExpensesFilter(sort = SortOrder.DATE_DESC).apply(list)
        assertEquals(today, result.first().date)
    }

    @Test
    fun `liste vide retourne une liste vide`() {
        val result = ExpensesFilter().apply(emptyList())
        assertTrue(result.isEmpty())
    }
}
