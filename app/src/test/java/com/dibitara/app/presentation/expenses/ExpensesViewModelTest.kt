package com.dibitara.app.presentation.expenses

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.AddTransactionUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase = mockk()
    private val addTransaction: AddTransactionUseCase = mockk()
    private lateinit var viewModel: ExpensesViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getMonthlyTransactions(any(), any()) } returns flowOf(emptyList())
        viewModel = ExpensesViewModel(getMonthlyTransactions, addTransaction)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `liste vide au démarrage`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is ExpensesUiState.Success } as ExpensesUiState.Success
        assertTrue(state.expenses.isEmpty())
        job.cancel()
    }

    @Test
    fun `n'affiche que les dépenses et pas les autres types`() = runTest {
        val transactions = listOf(
            buildTransaction(type = TransactionType.EXPENSE),
            buildTransaction(type = TransactionType.INCOME),
            buildTransaction(type = TransactionType.INVESTMENT)
        )
        every { getMonthlyTransactions(any(), any()) } returns flowOf(transactions)
        viewModel = ExpensesViewModel(getMonthlyTransactions, addTransaction)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is ExpensesUiState.Success } as ExpensesUiState.Success
        assertEquals(1, state.expenses.size)
        assertEquals(TransactionType.EXPENSE, state.expenses.first().type)
        job.cancel()
    }

    @Test
    fun `addExpense avec montant valide émet événement Added`() = runTest {
        coEvery { addTransaction(any()) } returns Result.success(1L)
        val events = mutableListOf<ExpensesEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addExpense("25.50", Category.ALIMENTATION, Currency.EUR, "Courses")
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is ExpensesEvent.Added })
        job.cancel()
    }

    @Test
    fun `addExpense avec montant invalide émet événement Error`() = runTest {
        val events = mutableListOf<ExpensesEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addExpense("abc", Category.ALIMENTATION, Currency.EUR, "")
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is ExpensesEvent.Error })
        job.cancel()
    }

    private fun buildTransaction(type: TransactionType) = Transaction(
        amountCents = 1000L, currency = Currency.EUR,
        category = Category.ALIMENTATION, type = type, date = LocalDate.now()
    )
}
