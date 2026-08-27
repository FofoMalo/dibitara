package com.dibitara.app.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToLong
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.usecase.CalculerTendanceActifUseCase
import com.dibitara.app.domain.usecase.DeleteChildUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsAccountUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAssetValuationHistoryUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
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
    private val ucSaveAssetSnapshot: SaveAssetValuationSnapshotUseCase,
    private val ucGetAssetValuationHistory: GetAssetValuationHistoryUseCase,
    private val ucCalculerTendanceActif: CalculerTendanceActifUseCase
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    val uiState: StateFlow<SavingsUiState> = combine(
        getSavings(),
        getChildren(),
        ucGetPreferences(),
        exchangeRateRepository.getRatesFlow()
    ) { accounts, children, prefs, rates ->
        // Conversion de chaque compte vers la devise par défaut avant sommation
        val target = prefs.deviseParDefaut
        val totalBalance = accounts.sumOf { CurrencyConverter.convertCents(it.currentBalanceCents, it.currency, target, rates) }
        val totalMonthly = accounts.sumOf { CurrencyConverter.convertCents(it.monthlyContributionCents, it.currency, target, rates) }
        val now = LocalDate.now()
        val totalVerse = getVersementsMois(CompteType.EPARGNE, now.year, now.monthValue)
            .sumOf { CurrencyConverter.convertCents(it.montantCents, it.currency, target, rates) }
        SavingsUiState.Success(
            accounts            = accounts,
            children            = children,
            totalEpargneCents   = totalBalance,
            totalMensuelCents   = totalMonthly,
            totalVerseMoisCents = totalVerse,
            summaryCurrency     = target
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
        plafondStr: String = ""
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val plafond = plafondStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
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
                    plafondCents = plafond
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
        plafondStr: String = ""
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val plafond = plafondStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
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
                    plafondCents             = plafond
                )
            )
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.SAVINGS_ACCOUNT, account.id, balance, currency)
                    _event.emit(SavingsEvent.Saved)
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
        val summaryCurrency     : Currency = Currency.EUR
    ) : SavingsUiState()
    data class Error(val message: String) : SavingsUiState()
}

sealed class SavingsEvent {
    data object Saved : SavingsEvent()
    data object Deleted : SavingsEvent()
    data object ChildSaved : SavingsEvent()
    data object VersementApplique : SavingsEvent()
    // Versement appliqué mais le nouveau solde dépasse le plafond configuré
    data class AvertissementPlafond(val compteLabel: String) : SavingsEvent()
    data class Error(val message: String) : SavingsEvent()
}
