package com.dibitara.app.presentation.settings

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.DuplicateGroup
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.DeleteTransactionUseCase
import com.dibitara.app.domain.usecase.DetectDuplicateTransactionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
class DuplicateCleanupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val detectDoublons: DetectDuplicateTransactionsUseCase = mockk()
    private val deleteTransaction: DeleteTransactionUseCase = mockk()

    private lateinit var viewModel: DuplicateCleanupViewModel

    private val date = LocalDate.of(2025, 3, 15)
    private val tx1 = buildTx(id = 1L, amountCents = 4250L)
    private val tx2 = buildTx(id = 2L, amountCents = 4250L)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial Loading puis Success avec groupes vides`() = runTest {
        every { detectDoublons() } returns flowOf(emptyList())
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        val state = viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
            as DuplicateCleanupUiState.Success

        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `groupes de doublons correctement exposés`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2))
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        val state = viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
            as DuplicateCleanupUiState.Success

        assertEquals(1, state.groups.size)
        assertEquals(2, state.groups[0].transactions.size)
        assertEquals(1L, state.groups[0].keepId)
    }

    @Test
    fun `changerSelection met à jour le keepId du groupe`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2))
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
        // On choisit de conserver tx2 au lieu de tx1
        viewModel.changerSelection(groupIndex = 0, keepId = 2L)

        val state = viewModel.uiState.value as DuplicateCleanupUiState.Success
        assertEquals(2L, state.groups[0].keepId)
    }

    @Test
    fun `supprimerDoublons appelle deleteTransaction pour les non-conservés`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepId = 1L)
        every { detectDoublons() } returns flowOf(listOf(groupe))
        // Après suppression, Room émettra une liste vide
        coEvery { deleteTransaction(tx2) } returns Result.success(Unit)
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }

        val events = mutableListOf<DuplicateCleanupEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.supprimerDoublons()
        testScheduler.advanceUntilIdle()

        // tx2 doit être supprimée, pas tx1
        coVerify(exactly = 1) { deleteTransaction(tx2) }
        coVerify(exactly = 0) { deleteTransaction(tx1) }
        assertTrue(events.any { it is DuplicateCleanupEvent.Supprime })
        job.cancel()
    }

    @Test
    fun `supprimerDoublons émet Erreur si deleteTransaction échoue`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepId = 1L)
        every { detectDoublons() } returns flowOf(listOf(groupe))
        coEvery { deleteTransaction(tx2) } returns Result.failure(RuntimeException("DB error"))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }

        val events = mutableListOf<DuplicateCleanupEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.supprimerDoublons()
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is DuplicateCleanupEvent.Erreur })
        job.cancel()
    }

    private fun buildTx(id: Long, amountCents: Long) = Transaction(
        id = id,
        amountCents = amountCents,
        currency = Currency.EUR,
        category = Category.ALIMENTATION,
        type = TransactionType.EXPENSE,
        date = date
    )
}
