package com.dibitara.app.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToLong
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.FundingMode
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.usecase.AnalyserPatrimoineUseCase
import com.dibitara.app.domain.usecase.AppliquerVersementsObjectifUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsGoalUseCase
import com.dibitara.app.domain.usecase.GetObjectifsVersementEnAttenteUseCase
import com.dibitara.app.domain.usecase.GetSavingsGoalsUseCase
import com.dibitara.app.domain.usecase.ProjectionObjectif
import com.dibitara.app.domain.usecase.ProjeterObjectifUseCase
import com.dibitara.app.domain.usecase.ResoudreMontantObjectifUseCase
import com.dibitara.app.domain.usecase.UpsertSavingsGoalUseCase
import com.dibitara.app.domain.usecase.CalculerTendanceActifUseCase
import com.dibitara.app.domain.usecase.DeleteChildUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsAccountUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAssetValuationHistoryUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
import com.dibitara.app.domain.usecase.GetVersementsEnAttenteUseCase
import com.dibitara.app.domain.usecase.GetVersementsMoisUseCase
import com.dibitara.app.domain.usecase.SaveAssetValuationSnapshotUseCase
import com.dibitara.app.domain.usecase.SaveChildUseCase
import com.dibitara.app.domain.usecase.SaveSavingsAccountUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateSavingsAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val getSavings: GetSavingsUseCase,
    private val saveSavingsAccount: SaveSavingsAccountUseCase,
    private val updateSavingsAccount: UpdateSavingsAccountUseCase,
    private val deleteSavingsAccount: DeleteSavingsAccountUseCase,
    private val getChildren: GetChildrenUseCase,
    private val saveChild: SaveChildUseCase,
    private val deleteChild: DeleteChildUseCase,
    private val saveVersement: SaveVersementUseCase,
    private val existeVersementMois: ExisteVersementMoisUseCase,
    private val getVersementsMois: GetVersementsMoisUseCase,
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val ucAnalyserPatrimoine: AnalyserPatrimoineUseCase,
    private val ucSaveAssetSnapshot: SaveAssetValuationSnapshotUseCase,
    private val ucGetAssetValuationHistory: GetAssetValuationHistoryUseCase,
    private val ucCalculerTendanceActif: CalculerTendanceActifUseCase,
    private val getSavingsGoals: GetSavingsGoalsUseCase,
    private val upsertSavingsGoal: UpsertSavingsGoalUseCase,
    private val deleteSavingsGoal: DeleteSavingsGoalUseCase,
    private val ucProjeterObjectif: ProjeterObjectifUseCase,
    private val ucGetVersementsEnAttente: GetVersementsEnAttenteUseCase,
    private val ucGetObjectifsVersementEnAttente: GetObjectifsVersementEnAttenteUseCase,
    private val ucAppliquerVersementsObjectif: AppliquerVersementsObjectifUseCase,
    private val ucResoudreMontantObjectif: ResoudreMontantObjectifUseCase
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    val uiState: StateFlow<SavingsUiState> = combine(
        getSavings(),
        getChildren(),
        ucGetPreferences(),
        exchangeRateRepository.getRatesFlow(),
        ucAnalyserPatrimoine()
    ) { accounts, children, prefs, rates, conseil ->
        // Conversion de chaque compte vers la devise par défaut avant sommation
        val target = prefs.deviseParDefaut
        val totalBalance = accounts.sumOf { CurrencyConverter.convertCents(it.currentBalanceCents, it.currency, target, rates) }
        val totalMonthly = accounts.sumOf { CurrencyConverter.convertCents(it.monthlyContributionCents, it.currency, target, rates) }
        val now = LocalDate.now()
        val versementsDuMois = getVersementsMois(CompteType.EPARGNE, now.year, now.monthValue)
        val totalVerse = versementsDuMois
            .sumOf { CurrencyConverter.convertCents(it.montantCents, it.currency, target, rates) }

        // Rappel in-app : comptes avec un versement mensuel prévu mais pas encore enregistré
        // ce mois-ci. Purement dérivé, rien de persisté. Aucun flux du combine n'observe
        // `monthly_versements` : ce bloc ne se recalcule que quand `savings_accounts` change
        // (getSavings() ré-émet). C'est suffisant car les deux écritures qui touchent les
        // versements touchent aussi les comptes - `appliquerVersement` (updateSavingsAccount)
        // et la restauration JSON (réécrit savings_accounts dans la même transaction).
        val comptesEnAttente = ucGetVersementsEnAttente(accounts, versementsDuMois)

        // Intérêts annuels estimés : Σ (solde × taux/100) de chaque compte, converti dans la devise cible.
        val interetsAnnuels = accounts.sumOf { compte ->
            val taux = compte.tauxAnnuelPct ?: 0.0
            if (taux <= 0.0) 0L
            else CurrencyConverter.convertCents(
                (compte.currentBalanceCents * taux / 100.0).roundToLong(),
                compte.currency, target, rates
            )
        }

        // Taux d'épargne réel : versements programmés / revenu moyen 3 mois (null si revenu inconnu).
        val tauxReel = conseil.revenuMoyenCents
            .takeIf { it > 0L }
            ?.let { totalMonthly.toFloat() / it.toFloat() }

        // Conversion secondaire du total pour la ligne "≈" : FCFA si la devise cible est €/$,
        // sinon € (pour ne pas afficher "≈ … FCFA" quand on est déjà en FCFA).
        val deviseSecondaire =
            if (target == Currency.XOF || target == Currency.XAF) Currency.EUR else Currency.XOF
        val conversionSecondaire =
            CurrencyConverter.convertCents(totalBalance, target, deviseSecondaire, rates)

        // Total épargné par enfant (devise cible) - affiché dans l'en-tête repliable de chaque enfant.
        val totauxParEnfant = children.associate { enfant ->
            enfant.id to accounts.filter { it.childId == enfant.id }
                .sumOf { CurrencyConverter.convertCents(it.currentBalanceCents, it.currency, target, rates) }
        }

        SavingsUiState.Success(
            accounts               = accounts,
            children               = children,
            totalEpargneCents      = totalBalance,
            totalMensuelCents      = totalMonthly,
            totalVerseMoisCents    = totalVerse,
            summaryCurrency        = target,
            liquiditesSuresCents   = conseil.liquiditesSuresCents,
            chargesMensuellesCents = conseil.chargesMensuellesCents,
            interetsAnnuelsCents   = interetsAnnuels,
            tauxEpargneReel        = tauxReel,
            tauxEpargneCiblePct    = prefs.tauxEpargneCiblePct,
            conversionSecondaireCents    = conversionSecondaire,
            conversionSecondaireCurrency = deviseSecondaire,
            totauxParEnfantCents         = totauxParEnfant,
            comptesVersementEnAttente    = comptesEnAttente
        ) as SavingsUiState
    }
        .catch { emit(SavingsUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavingsUiState.Loading
        )

    private val _event = MutableSharedFlow<SavingsEvent>()
    val event: SharedFlow<SavingsEvent> = _event.asSharedFlow()

    /**
     * Objectifs d'épargne (§3), exposés hors du `combine` de [uiState] : celui-ci est
     * déjà à 5 flows (limite des surcharges typées de `combine`). Chaque objectif est
     * accompagné de sa projection de date (calcul pur, pas de devise convertie - un
     * objectif s'affiche dans sa propre devise) et d'un flag « versement du mois pas
     * encore enregistré » (rappel in-app).
     *
     * Réactivité : aucun flux du combine n'observe `monthly_versements`. Ce bloc se
     * recalcule quand `getSavingsGoals()` ré-émet, ce qui arrive après un
     * `appliquerVersementsObjectif` (upsert de l'objectif) comme après une
     * restauration (réécriture de `savings_goals`) - et quand `getSavings()`/les taux
     * changent, pour un objectif en `FundingMode.SOLDE_COMPTE`.
     *
     * `ObjectifUi.goal` porte le montant RÉSOLU (`ucResoudreMontantObjectif`), pas la
     * valeur brute en base : pour SOLDE_COMPTE c'est un `copy()` d'affichage, jamais
     * persisté. Ce choix a un effet de bord voulu - voir CADRAGE_OBJECTIFS_CONNECTES.md
     * Q2 : rouvrir la feuille d'édition pré-remplit `existant.currentAmountCents` avec ce
     * montant déjà résolu, donc délier le compte (retour à MANUEL) fige naturellement la
     * dernière valeur affichée, sans code de gel dédié dans `upsertObjectif`.
     */
    val objectifs: StateFlow<List<ObjectifUi>> =
        combine(getSavingsGoals(), getSavings(), exchangeRateRepository.getRatesFlow()) { goals, comptes, rates ->
            val now = LocalDate.now()
            val versesCeMois = getVersementsMois(CompteType.OBJECTIF, now.year, now.monthValue)
            val enAttente = ucGetObjectifsVersementEnAttente(goals, versesCeMois).toSet()
            goals.map { goal ->
                val goalResolu = goal.copy(currentAmountCents = ucResoudreMontantObjectif(goal, comptes, rates))
                ObjectifUi(
                    goal = goalResolu,
                    projection = ucProjeterObjectif(goalResolu),
                    // En SOLDE_COMPTE le bouton "Verser" est masqué (Q3 du cadrage, voir
                    // ObjectifCard) - inutile d'allumer son rappel "à enregistrer".
                    versementEnAttente = goal.id in enAttente && goal.fundingModeEffectif != FundingMode.SOLDE_COMPTE
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Tendance d'un compte épargne, lue à la demande - même logique que
     * [com.dibitara.app.presentation.investments.InvestmentsViewModel.tendancePourActif].
     */
    suspend fun tendancePourActif(assetId: Long): Float? =
        ucCalculerTendanceActif(ucGetAssetValuationHistory(AssetValuationType.SAVINGS_ACCOUNT, assetId).first())

    fun saveAccount(
        type: SavingsType,
        label: String,
        balanceStr: String,
        contributionStr: String,
        currency: Currency,
        childId: Long?,
        plafondStr: String = "",
        tauxStr: String = ""
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val plafond = plafondStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        val taux = tauxStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
        viewModelScope.launch {
            saveSavingsAccount(
                SavingsAccount(
                    type = type,
                    label = label,
                    currentBalanceCents = balance,
                    monthlyContributionCents = contribution,
                    currency = currency,
                    childId = childId,
                    updatedAt = LocalDate.now(),
                    plafondCents = plafond,
                    tauxAnnuelPct = taux
                )
            )
                .onSuccess { newId ->
                    ucSaveAssetSnapshot(AssetValuationType.SAVINGS_ACCOUNT, newId, balance, currency)
                    _event.emit(SavingsEvent.Saved)
                }
                .onFailure { _event.emit(SavingsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateAccount(
        account: SavingsAccount,
        type: SavingsType,
        label: String,
        balanceStr: String,
        contributionStr: String,
        currency: Currency,
        childId: Long?,
        plafondStr: String = "",
        tauxStr: String = ""
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val plafond = plafondStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        val taux = tauxStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
        viewModelScope.launch {
            updateSavingsAccount(
                account.copy(
                    type                     = type,
                    label                    = label,
                    currentBalanceCents      = balance,
                    monthlyContributionCents = contribution,
                    currency                 = currency,
                    childId                  = childId,
                    updatedAt                = LocalDate.now(),
                    plafondCents             = plafond,
                    tauxAnnuelPct            = taux
                )
            )
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.SAVINGS_ACCOUNT, account.id, balance, currency)
                    _event.emit(SavingsEvent.Saved)
                }
                .onFailure { _event.emit(SavingsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    /**
     * Crée ([existant] == null) ou met à jour un objectif d'épargne.
     * Le montant objectif est obligatoire et strictement positif ; le montant épargné
     * et le versement mensuel valent 0 s'ils sont vides. Montants parsés `,`→`.`.
     *
     * [currentStr] est toujours écrit tel quel en base, y compris en SOLDE_COMPTE : ce
     * mode ne LIT jamais `currentAmountCents` (voir `ResoudreMontantObjectifUseCase`), la
     * valeur stockée ne redevient significative qu'après déliaison. Comme [existant] vient
     * de l'`ObjectifUi` affiché (déjà résolu), rouvrir la feuille pré-remplit ce champ avec
     * le solde du compte au moment de l'ouverture - c'est ce qui fige la valeur à la
     * déliaison (Q2 du cadrage), sans logique dédiée ici.
     */
    fun upsertObjectif(
        existant: SavingsGoal?,
        name: String,
        targetStr: String,
        currentStr: String,
        monthlyStr: String,
        currency: Currency,
        targetDate: LocalDate,
        color: GoalColor,
        icon: GoalIcon,
        sourceAccountId: Long?,
        fundingMode: FundingMode
    ) {
        val target = targetStr.replace(',', '.').toDoubleOrNull()
            ?.let { (it * 100).roundToLong() }
            ?.takeIf { it > 0L }
            ?: run {
                viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant objectif invalide")) }
                return
            }
        val current = currentStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val monthly = monthlyStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L

        val goal = SavingsGoal(
            id = existant?.id ?: 0,
            name = name, targetAmountCents = target, currentAmountCents = current,
            targetDate = targetDate, monthlyContributionCents = monthly,
            currency = currency, colorKey = color, iconKey = icon,
            sourceAccountId = sourceAccountId.takeIf { fundingMode == FundingMode.SOLDE_COMPTE },
            fundingMode = fundingMode
        )
        viewModelScope.launch {
            runCatching { upsertSavingsGoal(goal) }
                .onSuccess { _event.emit(SavingsEvent.ObjectifEnregistre) }
                .onFailure { _event.emit(SavingsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteObjectif(goal: SavingsGoal) {
        viewModelScope.launch {
            deleteSavingsGoal(goal)
            _event.emit(SavingsEvent.ObjectifSupprime)
        }
    }

    /**
     * Enregistre [nbMensualites] versements sur [goal] (mois courant + rattrapage des
     * mois manqués), avance sa progression, puis émet un événement pour le retour visuel.
     */
    fun appliquerVersementsObjectif(goal: SavingsGoal, nbMensualites: Int) {
        viewModelScope.launch {
            ucAppliquerVersementsObjectif(goal, nbMensualites)
                .onSuccess { res ->
                    _event.emit(
                        SavingsEvent.VersementObjectifApplique(
                            mensualites = res.mensualitesEnregistrees,
                            montantCents = res.centimesCredites,
                            currency = goal.currency
                        )
                    )
                }
                .onFailure { _event.emit(SavingsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteAccount(account: SavingsAccount) {
        viewModelScope.launch {
            deleteSavingsAccount(account)
            _event.emit(SavingsEvent.Deleted)
        }
    }

    fun addChild(name: String) {
        viewModelScope.launch {
            saveChild(Child(name = name))
                .onSuccess { _event.emit(SavingsEvent.ChildSaved) }
                .onFailure { _event.emit(SavingsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun removeChild(child: Child) {
        viewModelScope.launch { deleteChild(child) }
    }

    /**
     * Associe ou désassocie des comptes épargne à un enfant.
     * - Les comptes dont l'ID est dans [comptesSelectionnes] sont liés à [child].
     * - Les comptes qui étaient déjà liés à [child] mais absents de la sélection sont délié (childId = null).
     * - Les comptes liés à un AUTRE enfant ne sont pas touchés.
     */
    fun associerComptesEnfant(child: Child, tousLesComptes: List<SavingsAccount>, comptesSelectionnes: Set<Long>) {
        viewModelScope.launch {
            tousLesComptes.forEach { compte ->
                val selectionne = compte.id in comptesSelectionnes
                if (selectionne && compte.childId != child.id) {
                    updateSavingsAccount(compte.copy(childId = child.id, updatedAt = LocalDate.now()))
                } else if (!selectionne && compte.childId == child.id) {
                    updateSavingsAccount(compte.copy(childId = null, updatedAt = LocalDate.now()))
                }
            }
        }
    }

    /**
     * Applique le versement mensuel prévu sur un compte épargne.
     * - Vérifie qu'aucun versement n'a déjà été enregistré ce mois.
     * - Enregistre le versement en base.
     * - Met à jour le solde du compte : Solde N = Solde N-1 + montant versement.
     * - Non-rétroactif : le montant enregistré est celui du compte AU MOMENT du versement.
     */
    fun appliquerVersement(account: SavingsAccount) {
        val now = LocalDate.now()
        viewModelScope.launch {
            if (existeVersementMois(account.id, CompteType.EPARGNE, now.year, now.monthValue)) {
                _event.emit(SavingsEvent.Error("Versement déjà enregistré pour ce mois"))
                return@launch
            }
            val versement = MonthlyVersement(
                accountId    = account.id,
                compteType   = CompteType.EPARGNE,
                year         = now.year,
                month        = now.monthValue,
                montantCents = account.monthlyContributionCents,
                currency     = account.currency
            )
            saveVersement(versement)
                .onSuccess {
                    val soldeApres = account.currentBalanceCents + account.monthlyContributionCents
                    updateSavingsAccount(
                        account.copy(
                            currentBalanceCents = soldeApres,
                            updatedAt           = now
                        )
                    )
                    // Avertir si le solde résultant dépasse le plafond configuré
                    val plafond = account.plafondCents
                    if (plafond != null && soldeApres > plafond) {
                        _event.emit(SavingsEvent.AvertissementPlafond(account.label))
                    } else {
                        _event.emit(SavingsEvent.VersementApplique)
                    }
                }
                .onFailure { _event.emit(SavingsEvent.Error("Vérifier les informations saisies")) }
        }
    }
}

sealed class SavingsUiState {
    data object Loading : SavingsUiState()
    data class Success(
        val accounts          : List<SavingsAccount>,
        val children          : List<Child>,
        val totalEpargneCents   : Long     = 0L,
        val totalMensuelCents   : Long     = 0L,
        val totalVerseMoisCents : Long     = 0L,
        val summaryCurrency     : Currency = Currency.EUR,
        // Fonds d'urgence (§2) - repris tel quel du Conseiller patrimoine (une seule définition).
        val liquiditesSuresCents   : Long = 0L,
        val chargesMensuellesCents : Long = 0L,
        // Hero (§1)
        val interetsAnnuelsCents : Long   = 0L,
        val tauxEpargneReel      : Float? = null,   // null = revenu moyen inconnu
        val tauxEpargneCiblePct  : Int    = 20,
        val conversionSecondaireCents    : Long     = 0L,
        val conversionSecondaireCurrency : Currency = Currency.XOF,
        // Total épargné par enfant (devise cible), pour l'en-tête repliable de chaque enfant.
        val totauxParEnfantCents         : Map<Long, Long> = emptyMap(),
        // Rappel in-app (§ versement mensuel) : ids des comptes dont le versement du mois
        // n'est pas encore enregistré. Dérivé, non persisté.
        val comptesVersementEnAttente    : List<Long> = emptyList()
    ) : SavingsUiState()
    data class Error(val message: String) : SavingsUiState()
}

/**
 * Un objectif d'épargne + sa projection de date + le rappel « versement du mois pas
 * encore enregistré », pour l'affichage.
 */
data class ObjectifUi(
    val goal: SavingsGoal,
    val projection: ProjectionObjectif,
    val versementEnAttente: Boolean = false
)

sealed class SavingsEvent {
    data object Saved : SavingsEvent()
    data object Deleted : SavingsEvent()
    data object ChildSaved : SavingsEvent()
    data object VersementApplique : SavingsEvent()
    data object ObjectifEnregistre : SavingsEvent()
    data object ObjectifSupprime : SavingsEvent()
    // Versement appliqué mais le nouveau solde dépasse le plafond configuré
    data class AvertissementPlafond(val compteLabel: String) : SavingsEvent()
    // Versement(s) enregistré(s) sur un objectif ; mensualites == 0 => objectif déjà à jour ce mois-ci
    data class VersementObjectifApplique(
        val mensualites: Int,
        val montantCents: Long,
        val currency: Currency
    ) : SavingsEvent()
    data class Error(val message: String) : SavingsEvent()
}
