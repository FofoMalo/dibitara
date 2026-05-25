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
    fun `groupes de doublons correctement exposés avec keepIds par défaut`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2))
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        val state = viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
            as DuplicateCleanupUiState.Success

        assertEquals(1, state.groups.size)
        assertEquals(2, state.groups[0].transactions.size)
        // Par défaut : seule la plus ancienne (id le plus petit) est conservée
        assertEquals(setOf(1L), state.groups[0].keepIds)
    }

    @Test
    fun `basculerSelection ajoute un id absent de keepIds`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2)) // keepIds = {1L}
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
        // On coche également tx2 → les deux doivent être conservées
        viewModel.basculerSelection(groupIndex = 0, transactionId = 2L)

        val state = viewModel.uiState.value as DuplicateCleanupUiState.Success
        assertEquals(setOf(1L, 2L), state.groups[0].keepIds)
    }

    @Test
    fun `basculerSelection retire un id déjà dans keepIds`() = runTest {
        // Groupe avec les deux transactions conservées au départ
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepIds = setOf(1L, 2L))
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
        // On décoche tx2 → seule tx1 reste dans keepIds
        viewModel.basculerSelection(groupIndex = 0, transactionId = 2L)

        val state = viewModel.uiState.value as DuplicateCleanupUiState.Success
        assertEquals(setOf(1L), state.groups[0].keepIds)
    }

    @Test
    fun `basculerSelection ne retire pas le dernier id conservé`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2)) // keepIds = {1L}
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }
        // Tenter de décocher le seul id conservé → doit être ignoré
        viewModel.basculerSelection(groupIndex = 0, transactionId = 1L)

        val state = viewModel.uiState.value as DuplicateCleanupUiState.Success
        assertEquals(setOf(1L), state.groups[0].keepIds)
    }

    @Test
    fun `supprimerDoublons supprime uniquement les transactions absentes de keepIds`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepIds = setOf(1L))
        every { detectDoublons() } returns flowOf(listOf(groupe))
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
    fun `supprimerDoublons ne supprime rien si toutes les transactions sont dans keepIds`() = runTest {
        // Les deux transactions cochées → faux positif géré par l'utilisateur
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepIds = setOf(1L, 2L))
        every { detectDoublons() } returns flowOf(listOf(groupe))
        viewModel = DuplicateCleanupViewModel(detectDoublons, deleteTransaction)

        viewModel.uiState.first { it is DuplicateCleanupUiState.Success }

        val events = mutableListOf<DuplicateCleanupEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.supprimerDoublons()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { deleteTransaction(any()) }
        assertTrue(events.any { it is DuplicateCleanupEvent.Supprime })
        job.cancel()
    }

    @Test
    fun `supprimerDoublons émet Erreur si deleteTransaction échoue`() = runTest {
        val groupe = DuplicateGroup(listOf(tx1, tx2), keepIds = setOf(1L))
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
