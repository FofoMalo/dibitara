package com.dibitara.app.presentation.report

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyReportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val ucGetReport: GetMonthlyReportUseCase = mockk()
    private lateinit var viewModel: MonthlyReportViewModel

    private val rapportVide = MonthlyReport(
        month = 5, year = 2026, currency = Currency.EUR,
        revenusCents = 0L, depensesCents = 0L, soldeCents = 0L,
        budget = null, topCategories = emptyList(), variationDepensesCents = 0L
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { ucGetReport(any(), any()) } returns flowOf(rapportVide)
        viewModel = MonthlyReportViewModel(ucGetReport)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial est Loading`() {
        // Vérifie que Loading est émis avant que le Flow produise sa valeur
        // (possible avec un dispatcher contrôlé)
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val vm = MonthlyReportViewModel(ucGetReport)
        assertEquals(MonthlyReportUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `état Success contient le rapport`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MonthlyReportUiState.Success)
        assertEquals(rapportVide, (state as MonthlyReportUiState.Success).report)
        job.cancel()
    }

    @Test
    fun `état Error émis en cas d exception`() = runTest {
        // L'exception doit être lancée DANS le Flow (pas par invoke()) pour être capturée par .catch
        every { ucGetReport(any(), any()) } returns flow { throw RuntimeException("Erreur base de données") }
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val vm = MonthlyReportViewModel(ucGetReport)

        val job = launch { vm.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is MonthlyReportUiState.Error)
        job.cancel()
    }
}
