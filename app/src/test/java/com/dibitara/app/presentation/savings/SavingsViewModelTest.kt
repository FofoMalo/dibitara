package com.dibitara.app.presentation.savings

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.FundingMode
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.SavingsGoal
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
import com.dibitara.app.domain.usecase.AppliquerVersementsObjectifUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsGoalUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetObjectifsVersementEnAttenteUseCase
import com.dibitara.app.domain.usecase.GetSavingsGoalsUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
import com.dibitara.app.domain.usecase.GetVersementsEnAttenteUseCase
import com.dibitara.app.domain.usecase.ProjeterObjectifUseCase
import com.dibitara.app.domain.usecase.ResoudreMontantObjectifUseCase
import com.dibitara.app.domain.usecase.VersementsObjectifResult
import com.dibitara.app.domain.usecase.GetVersementsMoisUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveAssetValuationSnapshotUseCase
import com.dibitara.app.domain.usecase.SaveChildUseCase
import com.dibitara.app.domain.usecase.SaveSavingsAccountUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateSavingsAccountUseCase
import com.dibitara.app.domain.usecase.UpsertSavingsGoalUseCase
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
    private val getSavingsGoals: GetSavingsGoalsUseCase = mockk()
    private val upsertSavingsGoal: UpsertSavingsGoalUseCase = mockk(relaxed = true)
    private val deleteSavingsGoal: DeleteSavingsGoalUseCase = mockk(relaxed = true)
    private val ucProjeterObjectif = ProjeterObjectifUseCase()
    private val ucGetVersementsEnAttente = GetVersementsEnAttenteUseCase()
    private val ucGetObjectifsVersementEnAttente = GetObjectifsVersementEnAttenteUseCase()
    private val ucAppliquerVersementsObjectif: AppliquerVersementsObjectifUseCase = mockk()
    private val ucResoudreMontantObjectif = ResoudreMontantObjectifUseCase()
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
        every { getSavingsGoals() } returns flowOf(emptyList())
        viewModel = SavingsViewModel(
            getSavings, saveSavingsAccount, updateSavingsAccount,
            deleteSavingsAccount, getChildren, saveChild, deleteChild,
            saveVersement, existeVersementMois, getVersementsMois, ucGetPreferences, ratesRepo,
            ucAnalyserPatrimoine, ucSaveAssetSnapshot, ucGetAssetValuationHistory, ucCalculerTendanceActif,
            getSavingsGoals, upsertSavingsGoal, deleteSavingsGoal, ucProjeterObjectif,
            ucGetVersementsEnAttente, ucGetObjectifsVersementEnAttente, ucAppliquerVersementsObjectif,
            ucResoudreMontantObjectif
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
            ucAnalyserPatrimoine, ucSaveAssetSnapshot, ucGetAssetValuationHistory, ucCalculerTendanceActif,
            getSavingsGoals, upsertSavingsGoal, deleteSavingsGoal, ucProjeterObjectif,
            ucGetVersementsEnAttente, ucGetObjectifsVersementEnAttente, ucAppliquerVersementsObjectif,
            ucResoudreMontantObjectif
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

    @Test
    fun `objectifs mappe chaque goal avec sa projection`() = runTest {
        every { getSavingsGoals() } returns flowOf(listOf(
            SavingsGoal(
                id = 1L, name = "Voiture", targetAmountCents = 1_000_000L,
                currentAmountCents = 200_000L, targetDate = LocalDate.now().plusMonths(24),
                monthlyContributionCents = 100_000L, currency = Currency.EUR,
                colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
            )
        ))
        rebuild()

        val objectifs = viewModel.objectifs.first { it.isNotEmpty() }

        assertEquals(1, objectifs.size)
        // reste 800 000 / 100 000 par mois = 8 mois
        assertEquals(8, objectifs.first().projection.moisRestants)
    }

    @Test
    fun `objectifs porte le flag versementEnAttente selon les versements OBJECTIF du mois`() = runTest {
        val goal = SavingsGoal(
            id = 1L, name = "Voiture", targetAmountCents = 1_000_000L, currentAmountCents = 0L,
            targetDate = LocalDate.now().plusMonths(12), monthlyContributionCents = 40_000L,
            currency = Currency.EUR, colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
        )
        every { getSavingsGoals() } returns flowOf(listOf(goal))
        val now = LocalDate.now()

        // Aucun versement ce mois-ci → en attente
        coEvery { getVersementsMois(CompteType.OBJECTIF, now.year, now.monthValue) } returns emptyList()
        rebuild()
        assertTrue(viewModel.objectifs.first { it.isNotEmpty() }.first().versementEnAttente)

        // Versement enregistré ce mois-ci → plus en attente
        coEvery { getVersementsMois(CompteType.OBJECTIF, now.year, now.monthValue) } returns listOf(
            MonthlyVersement(
                id = 1L, accountId = 1L, compteType = CompteType.OBJECTIF,
                year = now.year, month = now.monthValue, montantCents = 40_000L, currency = Currency.EUR
            )
        )
        rebuild()
        assertFalse(viewModel.objectifs.first { it.isNotEmpty() }.first().versementEnAttente)
    }

    @Test
    fun `objectifs en SOLDE_COMPTE resout le montant depuis le compte lie, pas la valeur stockee`() = runTest {
        // currentAmountCents = 0 volontairement : si la résolution ne se déclenchait pas,
        // ce test échouerait sur la progression (0 %) plutôt que de passer par accident.
        val goal = SavingsGoal(
            id = 1L, name = "Cesar", targetAmountCents = 1_000_000L, currentAmountCents = 0L,
            targetDate = LocalDate.now().plusMonths(12), monthlyContributionCents = 0L,
            currency = Currency.EUR, colorKey = GoalColor.TEAL, iconKey = GoalIcon.AUTRE,
            sourceAccountId = 9L, fundingMode = FundingMode.SOLDE_COMPTE
        )
        val compte = SavingsAccount(
            id = 9L, type = SavingsType.LIVRET_A, label = "Epargne Cesar",
            currentBalanceCents = 350_000L, monthlyContributionCents = 0L,
            currency = Currency.EUR, updatedAt = LocalDate.now()
        )
        every { getSavingsGoals() } returns flowOf(listOf(goal))
        every { getSavings() } returns flowOf(listOf(compte))
        rebuild()

        val resultat = viewModel.objectifs.first { it.isNotEmpty() }.first()
        assertEquals(350_000L, resultat.goal.currentAmountCents)
        assertEquals(0.35f, resultat.goal.progression)
    }

    @Test
    fun `objectifs masque versementEnAttente en SOLDE_COMPTE - le bouton Verser n'existe plus`() = runTest {
        val goal = SavingsGoal(
            id = 1L, name = "Cesar", targetAmountCents = 1_000_000L, currentAmountCents = 0L,
            targetDate = LocalDate.now().plusMonths(12), monthlyContributionCents = 40_000L,
            currency = Currency.EUR, colorKey = GoalColor.TEAL, iconKey = GoalIcon.AUTRE,
            sourceAccountId = 9L, fundingMode = FundingMode.SOLDE_COMPTE
        )
        every { getSavingsGoals() } returns flowOf(listOf(goal))
        every { getSavings() } returns flowOf(emptyList())
        val now = LocalDate.now()
        coEvery { getVersementsMois(CompteType.OBJECTIF, now.year, now.monthValue) } returns emptyList()
        rebuild()

        assertFalse(viewModel.objectifs.first { it.isNotEmpty() }.first().versementEnAttente)
    }

    @Test
    fun `appliquerVersementsObjectif émet VersementObjectifApplique avec le résultat`() = runTest {
        val goal = SavingsGoal(
            id = 7L, name = "Voiture", targetAmountCents = 1_500_000L, currentAmountCents = 420_000L,
            targetDate = LocalDate.now().plusMonths(12), monthlyContributionCents = 40_000L,
            currency = Currency.EUR, colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
        )
        coEvery { ucAppliquerVersementsObjectif(goal, 3) } returns
            Result.success(VersementsObjectifResult(mensualitesEnregistrees = 3, centimesCredites = 120_000L))
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.appliquerVersementsObjectif(goal, 3)
        testScheduler.advanceUntilIdle()

        val evt = events.filterIsInstance<SavingsEvent.VersementObjectifApplique>().single()
        assertEquals(3, evt.mensualites)
        assertEquals(120_000L, evt.montantCents)
        job.cancel()
    }

    @Test
    fun `upsertObjectif avec montant objectif valide émet ObjectifEnregistre`() = runTest {
        coEvery { upsertSavingsGoal(any()) } just Runs
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.upsertObjectif(
            null, "Vacances", "3000", "500", "150", Currency.EUR,
            LocalDate.now().plusMonths(10), GoalColor.OR, GoalIcon.VOYAGE,
            null, FundingMode.MANUEL
        )
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.ObjectifEnregistre })
        job.cancel()
    }

    @Test
    fun `upsertObjectif en édition conserve l'id de l'objectif existant`() = runTest {
        // Garde-fou : SavingsGoalDao.upsert est @Insert(REPLACE) sur une PK autoGenerate.
        // Un id perdu (= 0) n'écrase pas la ligne, il en insère une nouvelle → doublon
        // silencieux à chaque « Modifier ». Ce test fige le passage de l'id.
        coEvery { upsertSavingsGoal(any()) } just Runs
        val existant = SavingsGoal(
            id = 7L, name = "Voiture", targetAmountCents = 1_000_000L,
            currentAmountCents = 200_000L, targetDate = LocalDate.now().plusMonths(12),
            monthlyContributionCents = 50_000L, currency = Currency.EUR,
            colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
        )

        viewModel.upsertObjectif(
            existant, "Voiture", "12000", "3000", "500", Currency.EUR,
            LocalDate.now().plusMonths(12), GoalColor.TEAL, GoalIcon.VOITURE,
            null, FundingMode.MANUEL
        )
        testScheduler.advanceUntilIdle()

        coVerify { upsertSavingsGoal(match { it.id == 7L && it.targetAmountCents == 1_200_000L }) }
    }

    @Test
    fun `upsertObjectif en SOLDE_COMPTE persiste le solde reel du compte, pas le champ masque`() = runTest {
        // Le champ "Montant déjà épargné" est masqué côté feuille en SOLDE_COMPTE : currentStr
        // arrive vide ("") comme si l'utilisateur n'avait rien saisi. Sans résolution au
        // moment de l'enregistrement, la ligne stockerait 0 - qui redeviendrait la valeur
        // affichée si le compte est supprimé plus tard.
        coEvery { upsertSavingsGoal(any()) } just Runs
        every { getSavings() } returns flowOf(listOf(
            SavingsAccount(
                id = 9L, type = SavingsType.LIVRET_A, label = "Epargne Cesar",
                currentBalanceCents = 350_000L, monthlyContributionCents = 0L,
                currency = Currency.EUR, updatedAt = LocalDate.now()
            )
        ))

        viewModel.upsertObjectif(
            null, "Cesar", "1000000", "", "0", Currency.EUR,
            LocalDate.now().plusMonths(12), GoalColor.TEAL, GoalIcon.AUTRE,
            9L, FundingMode.SOLDE_COMPTE
        )
        testScheduler.advanceUntilIdle()

        coVerify { upsertSavingsGoal(match { it.currentAmountCents == 350_000L && it.sourceAccountId == 9L }) }
    }

    @Test
    fun `upsertObjectif avec montant objectif nul ou vide émet Error`() = runTest {
        val events = mutableListOf<SavingsEvent>()
        val job = launch(testDispatcher) { viewModel.event.collect { events.add(it) } }

        viewModel.upsertObjectif(
            null, "Vacances", "0", "", "", Currency.EUR,
            LocalDate.now().plusMonths(10), GoalColor.OR, GoalIcon.VOYAGE,
            null, FundingMode.MANUEL
        )
        testScheduler.advanceUntilIdle()

        assertTrue(events.any { it is SavingsEvent.Error })
        coVerify(exactly = 0) { upsertSavingsGoal(any()) }
        job.cancel()
    }

    @Test
    fun `comptesVersementEnAttente liste les comptes avec versement mensuel non encore enregistré`() = runTest {
        every { getSavings() } returns flowOf(listOf(
            buildAccount(contribution = 20_000L).copy(id = 1L),
            buildAccount(contribution = 0L).copy(id = 2L)   // pas de versement mensuel → jamais en attente
        ))
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(listOf(1L), state.comptesVersementEnAttente)
    }

    @Test
    fun `comptesVersementEnAttente exclut un compte déjà versé ce mois-ci`() = runTest {
        val now = LocalDate.now()
        every { getSavings() } returns flowOf(listOf(buildAccount(contribution = 20_000L).copy(id = 1L)))
        coEvery { getVersementsMois(CompteType.EPARGNE, now.year, now.monthValue) } returns listOf(
            MonthlyVersement(
                id = 1L, accountId = 1L, compteType = CompteType.EPARGNE,
                year = now.year, month = now.monthValue, montantCents = 20_000L, currency = Currency.EUR
            )
        )
        rebuild()

        val state = viewModel.uiState.first { it is SavingsUiState.Success } as SavingsUiState.Success

        assertEquals(emptyList<Long>(), state.comptesVersementEnAttente)
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
