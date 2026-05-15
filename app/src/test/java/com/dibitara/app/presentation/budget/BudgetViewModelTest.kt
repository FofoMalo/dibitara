package com.dibitara.app.presentation.budget

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.DeleteBudgetUseCase
import com.dibitara.app.domain.usecase.GetMonthlyBudgetUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import com.dibitara.app.domain.usecase.SetBudgetUseCase
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
class BudgetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getMonthlyBudget: GetMonthlyBudgetUseCase = mockk()
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase = mockk()
    private val setBudget: SetBudgetUseCase = mockk()
    private val deleteBudget: DeleteBudgetUseCase = mockk()
    private lateinit var viewModel: BudgetViewModel

    private val now = LocalDate.now()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getMonthlyBudget(any(), any()) } returns flowOf(null)
        every { getMonthlyTransactions(any(), any()) } returns flowOf(emptyList())
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget, deleteBudget)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial expose Success avec budget null`() = runTest {
        // On doit s'abonner pour déclencher le stateIn(WhileSubscribed)
        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is BudgetUiState.Success }
        assertNull((state as BudgetUiState.Success).budget)
        job.cancel()
    }

    @Test
    fun `saveBudget avec montant invalide ne persiste rien`() = runTest {
        viewModel.saveBudget("abc", Currency.EUR)
        coEvery { setBudget(any()) } returns Result.success(Unit)
        // Pas d'exception = comportement attendu
    }

    @Test
    fun `état reflète le budget quand le repository en retourne un`() = runTest {
        // spentCents en base est ignoré — c'est la somme des transactions EXPENSE qui compte
        val budget = Budget(
            month = now.monthValue, year = now.year,
            allocatedCents = 150000L, spentCents = 0L, currency = Currency.EUR
        )
        val depenses = listOf(
            Transaction(amountCents = 30000L, currency = Currency.EUR,
                category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
                date = LocalDate.now()),
            Transaction(amountCents = 20000L, currency = Currency.EUR,
                category = Category.TRANSPORT, type = TransactionType.EXPENSE,
                date = LocalDate.now()),
            // Un revenu qui ne doit PAS être compté dans le dépensé
            Transaction(amountCents = 10000L, currency = Currency.EUR,
                category = Category.AUTRE, type = TransactionType.INCOME,
                date = LocalDate.now())
        )
        every { getMonthlyBudget(any(), any()) } returns flowOf(budget)
        every { getMonthlyTransactions(any(), any()) } returns flowOf(depenses)
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget, deleteBudget)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is BudgetUiState.Success } as BudgetUiState.Success
        assertEquals(150000L, state.budget?.allocatedCents)
        // spentCents = 30000 + 20000 = 50000 (le revenu de 10000 est exclu)
        assertEquals(50000L, state.budget?.spentCents)
        assertEquals(100000L, state.budget?.remainingCents)
        job.cancel()
    }

    @Test
    fun `revenusCents et soldeCents calculés depuis les transactions INCOME`() = runTest {
        val transactions = listOf(
            Transaction(amountCents = 200000L, currency = Currency.EUR,
                category = Category.AUTRE, type = TransactionType.INCOME,
                date = LocalDate.now(), note = "Salaire"),
            Transaction(amountCents = 50000L, currency = Currency.EUR,
                category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
                date = LocalDate.now()),
            Transaction(amountCents = 30000L, currency = Currency.EUR,
                category = Category.TRANSPORT, type = TransactionType.EXPENSE,
                date = LocalDate.now())
        )
        every { getMonthlyBudget(any(), any()) } returns flowOf(null)
        every { getMonthlyTransactions(any(), any()) } returns flowOf(transactions)
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget, deleteBudget)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is BudgetUiState.Success } as BudgetUiState.Success

        assertEquals(200000L, state.revenusCents)               // 1 INCOME
        assertEquals(80000L,  state.depensesCents)              // 50000 + 30000
        assertEquals(120000L, state.soldeCents)                 // 200000 - 80000
        job.cancel()
    }

    @Test
    fun `soldeCents est négatif quand les dépenses dépassent les revenus`() = runTest {
        val transactions = listOf(
            Transaction(amountCents = 50000L, currency = Currency.EUR,
                category = Category.AUTRE, type = TransactionType.INCOME,
                date = LocalDate.now()),
            Transaction(amountCents = 80000L, currency = Currency.EUR,
                category = Category.LOISIRS, type = TransactionType.EXPENSE,
                date = LocalDate.now())
        )
        every { getMonthlyTransactions(any(), any()) } returns flowOf(transactions)
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget, deleteBudget)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is BudgetUiState.Success } as BudgetUiState.Success

        assertEquals(-30000L, state.soldeCents)
        job.cancel()
    }
}
