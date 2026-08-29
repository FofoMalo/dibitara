package com.dibitara.app.presentation.savings

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.model.ConseilPatrimoineResult
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.AnalyserPatrimoineUseCase
import com.dibitara.app.domain.usecase.CalculerTendanceActifUseCase
import com.dibitara.app.domain.usecase.DeleteChildUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsAccountUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAssetValuationHistoryUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
import com.dibitara.app.domain.usecase.GetVersementsMoisUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveAssetValuationSnapshotUseCase
import com.dibitara.app.domain.usecase.SaveChildUseCase
import com.dibitara.app.domain.usecase.SaveSavingsAccountUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateSavingsAccountUseCase
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
class SavingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getSavings: GetSavingsUseCase = mockk()
    private val saveSavingsAccount: SaveSavingsAccountUseCase = mockk()
    private val updateSavingsAccount: UpdateSavingsAccountUseCase = mockk()
    private val deleteSavingsAccount: DeleteSavingsAccountUseCase = mockk()
    private val getChildren: GetChildrenUseCase = mockk()
    private val saveChild: SaveChildUseCase = mockk()
    private val deleteChild: DeleteChildUseCase = mockk()
    private val saveVersement: SaveVersementUseCase = mockk()
    private val existeVersementMois: ExisteVersementMoisUseCase = mockk()
    private val getVersementsMois: GetVersementsMoisUseCase = mockk()
    private val ucGetPreferences: GetUserPreferencesUseCase = mockk()
    private val ratesRepo: ExchangeRateRepository = mockk()
    private val ucAnalyserPatrimoine: AnalyserPatrimoineUseCase = mockk()
    private val ucSaveAssetSnapshot: SaveAssetValuationSnapshotUseCase = mockk(relaxed = true)
    private val ucGetAssetValuationHistory: GetAssetValuationHistoryUseCase = mockk(relaxed = true)
    private val ucCalculerTendanceActif: CalculerTendanceActifUseCase = mockk(relaxed = true)
    private lateinit var viewModel: SavingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSavings() } returns flowOf(emptyList())
        every { getChildren() } returns flowOf(emptyList())
        every { ucGetPreferences() } returns flowOf(UserPreferences())
        every { ratesRepo.getRatesFlow() } returns flowOf(ExchangeRates(1.09, 655.96, 0L))
        every { ucAnalyserPatrimoine(any(), any()) } returns flowOf(conseil())
        coEvery { getVersementsMois(any(), any(), any()) } returns emptyList()
        viewModel = SavingsViewModel(
            getSavings, saveSavingsAccount, updateSavingsAccount,
            deleteSavingsAccount, getChildren, saveChild, deleteChild,
            saveVersement, existeVersementMois, getVersementsMois, ucGetPreferences, ratesRepo,
            ucAnalyserPatrimoine, ucSaveAssetSnapshot, ucGetAssetValuationHistory, ucCalculerTendanceActif
        )
    }

    private fun conseil(
        revenuMoyenCents: Long = 0L,
        chargesMensuellesCents: Long = 0L,
        liquiditesSuresCents: Long = 0L
    ) = ConseilPatrimoineResult(
        currency = Currency.EUR,
        liquiditesSuresCents = liquiditesSuresCents,
        objectifPrecautionCents = chargesMensuellesCents * 6,
        precautionSuffisante = liquiditesSuresCents >= chargesMensuellesCents * 6,
        revenuMoyenCents = revenuMoyenCents,
        chargesMensuellesCents = chargesMensuellesCents,
        pochesAvecMarge = emptyList(),
        objectifEpargneMensuelCents = 0L,
        resteAVivreReelCents = 0L,
        versementsProgrammesCents = 0L,
        capaciteNonAffecteeCents = 0L,
        objectifPlafonneParResteAVivre = false,
        repartitionParCategorie = emptyList(),
        categorieSurConcentree = null
    )

    /** Les flows (getSavings, prefs, analyse...) sont capturés à la construction du VM :
     *  toute stub qui les change doit être suivie d'un rebuild(). */
    private fun rebuild() {
        viewModel = SavingsViewModel(
            getSavings, saveSavingsAccount, updateSavingsAccount,
            deleteSavingsAccount, getChildren, saveChild, deleteChild,
            saveVersement, existeVersementMois, getVersementsMois, ucGetPreferences, ratesRepo,
            ucAnalyserPatrimoine, ucSaveAssetSnapshot, ucGetAssetValuationHistory, ucCalculerTendanceActif
        )
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `saveAccount avec montant valide émet événement Saved`() = runTest {
        coEvery { saveSavingsAccount(any()) } returns Result.success(1L)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.saveAccount(SavingsType.LIVRET_A, "Livret A", "5000.00", "200.00", Currency.EUR, null)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `addChild avec nom valide émet ChildSaved`() = runTest {
        coEvery { saveChild(any()) } returns Result.success(1L)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.addChild("Emma")
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.ChildSaved })
        job.cancel()
    }

    @Test
    fun `updateAccount avec montant valide émet événement Saved`() = runTest {
        coEvery { updateSavingsAccount(any()) } returns Result.success(Unit)
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.updateAccount(buildAccount(), SavingsType.LIVRET_A, "Livret A modifié", "6000.00", "300.00", Currency.EUR, null)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Saved })
        job.cancel()
    }

    @Test
    fun `deleteAccount appelle le usecase et émet Deleted`() = runTest {
        val compte = buildAccount()
        coEvery { deleteSavingsAccount(any()) } just Runs
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.deleteAccount(compte)
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Deleted })
        job.cancel()
    }

    @Test
    fun `uiState expose le total verse ce mois converti dans la devise par defaut`() = runTest {
        coEvery { getVersementsMois(CompteType.EPARGNE, any(), any()) } returns listOf(
            MonthlyVersement(
                id = 1L, accountId = 1L, compteType = CompteType.EPARGNE,
                year = LocalDate.now().year, month = LocalDate.now().monthValue,
                montantCents = 20000L, currency = Currency.EUR
            )
        )

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(20000L, state.totalVerseMoisCents)
    }

    @Test
    fun `fonds d'urgence expose liquidites et charges du Conseiller patrimoine`() = runTest {
        every { ucAnalyserPatrimoine(any(), any()) } returns flowOf(
            conseil(liquiditesSuresCents = 900_000L, chargesMensuellesCents = 150_000L)
        )
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(900_000L, state.liquiditesSuresCents)
        assertEquals(150_000L, state.chargesMensuellesCents)
    }

    @Test
    fun `taux d'epargne reel est versements sur revenu moyen`() = runTest {
        every { getSavings() } returns flowOf(listOf(buildAccount(contribution = 30_000L)))
        every { ucAnalyserPatrimoine(any(), any()) } returns flowOf(conseil(revenuMoyenCents = 300_000L))
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(0.10f, state.tauxEpargneReel!!, 0.001f)
    }

    @Test
    fun `taux d'epargne reel est null si revenu moyen inconnu`() = runTest {
        every { getSavings() } returns flowOf(listOf(buildAccount(contribution = 30_000L)))
        every { ucAnalyserPatrimoine(any(), any()) } returns flowOf(conseil(revenuMoyenCents = 0L))
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertNull(state.tauxEpargneReel)
    }

    @Test
    fun `interets annuels estimes = somme solde fois taux, 0 si taux absent`() = runTest {
        every { getSavings() } returns flowOf(listOf(
            buildAccount(balance = 1_000_000L).copy(tauxAnnuelPct = 3.0),   // 30 000
            buildAccount(balance = 500_000L).copy(tauxAnnuelPct = null)     // 0
        ))
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(30_000L, state.interetsAnnuelsCents)
    }

    @Test
    fun `conversion secondaire = FCFA quand devise cible EUR, EUR quand devise cible FCFA`() = runTest {
        every { getSavings() } returns flowOf(listOf(buildAccount(balance = 100_00L)))
        rebuild()

        val enEuro = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success
        assertEquals(Currency.XOF, enEuro.conversionSecondaireCurrency)

        every { ucGetPreferences() } returns flowOf(UserPreferences(deviseParDefaut = Currency.XOF))
        rebuild()

        val enFcfa = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success
        assertEquals(Currency.EUR, enFcfa.conversionSecondaireCurrency)
    }

    private fun buildAccount(balance: Long = 500000L, contribution: Long = 20000L) = SavingsAccount(
        id = 1L,
        type = SavingsType.LIVRET_A,
        label = "Livret A",
        currentBalanceCents = balance,
        monthlyContributionCents = contribution,
        currency = Currency.EUR,
        childId = null,
        updatedAt = LocalDate.now()
    )
}
