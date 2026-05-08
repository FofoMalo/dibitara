package com.dibitara.app.presentation.debts

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.usecase.DeleteDebtUseCase
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.SaveDebtUseCase
import io.mockk.*
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
class DebtsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getDebts: GetDebtsUseCase = mockk()
    private val saveDebt: SaveDebtUseCase = mockk()
    private val deleteDebt: DeleteDebtUseCase = mockk()
    private lateinit var viewModel: DebtsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getDebts() } returns flowOf(emptyList())
        viewModel = DebtsViewModel(getDebts, saveDebt, deleteDebt)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial est Loading puis Success avec liste vide`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is DebtsUiState.Success } as DebtsUiState.Success
        assertTrue(state.debts.isEmpty())
        job.cancel()
    }

    @Test
    fun `liste reflète les dettes retournées par le repository`() = runTest {
        val dettes = listOf(buildDebt("Crédit auto"), buildDebt("Crédit immo"))
        every { getDebts() } returns flowOf(dettes)
        viewModel = DebtsViewModel(getDebts, saveDebt, deleteDebt)

        val job = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it is DebtsUiState.Success } as DebtsUiState.Success
        assertEquals(2, state.debts.size)
        job.cancel()
    }

    @Test
    fun `addDebt avec montant valide émet événement Saved`() = runTest {
        coEvery { saveDebt(any()) } returns Result.success(1L)
        val events = mutableListOf<DebtsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addDebt("Crédit auto", "10000.00", "250.00", Currency.EUR, DebtType.CREDIT_CONSO)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is DebtsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `removeDebt appelle le usecase et émet Deleted`() = runTest {
        val dette = buildDebt("Crédit immo")
        coEvery { deleteDebt(any()) } just Runs
        val events = mutableListOf<DebtsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.removeDebt(dette)
        testScheduler.advanceUntilIdle()

        coVerify { deleteDebt(dette) }
        assertTrue(events.any { it is DebtsEvent.Deleted })
        job.cancel()
    }

    private fun buildDebt(label: String) = Debt(
        id = 1L,
        label = label,
        totalCents = 1000000L,
        monthlyPaymentCents = 50000L,
        currency = Currency.EUR,
        type = DebtType.CREDIT_IMMO,
        updatedAt = LocalDate.now()
    )
}
