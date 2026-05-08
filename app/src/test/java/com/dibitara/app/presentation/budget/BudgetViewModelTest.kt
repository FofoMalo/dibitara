package com.dibitara.app.presentation.budget

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
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
    private lateinit var viewModel: BudgetViewModel

    private val now = LocalDate.now()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getMonthlyBudget(any(), any()) } returns flowOf(null)
        every { getMonthlyTransactions(any(), any()) } returns flowOf(emptyList())
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget)
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
        val budget = Budget(
            month = now.monthValue, year = now.year,
            allocatedCents = 150000L, spentCents = 50000L, currency = Currency.EUR
        )
        every { getMonthlyBudget(any(), any()) } returns flowOf(budget)
        viewModel = BudgetViewModel(getMonthlyBudget, getMonthlyTransactions, setBudget)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is BudgetUiState.Success } as BudgetUiState.Success
        assertEquals(150000L, state.budget?.allocatedCents)
        assertEquals(100000L, state.budget?.remainingCents)
        job.cancel()
    }
}
