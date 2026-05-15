package com.dibitara.app.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.usecase.DeleteChildUseCase
import com.dibitara.app.domain.usecase.DeleteSavingsAccountUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetChildrenUseCase
import com.dibitara.app.domain.usecase.GetSavingsUseCase
import com.dibitara.app.domain.usecase.SaveChildUseCase
import com.dibitara.app.domain.usecase.SaveSavingsAccountUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
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
    private val existeVersementMois: ExisteVersementMoisUseCase
) : ViewModel() {

    val uiState: StateFlow<SavingsUiState> = combine(
        getSavings(),
        getChildren()
    ) { accounts, children ->
        SavingsUiState.Success(accounts = accounts, children = children) as SavingsUiState
    }
        .catch { emit(SavingsUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavingsUiState.Loading
        )

    private val _event = MutableSharedFlow<SavingsEvent>()
    val event: SharedFlow<SavingsEvent> = _event.asSharedFlow()

    fun saveAccount(
        type: SavingsType,
        label: String,
        balanceStr: String,
        contributionStr: String,
        currency: Currency,
        childId: Long?
    ) {
        val balance = balanceStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            saveSavingsAccount(
                SavingsAccount(
                    type = type,
                    label = label,
                    currentBalanceCents = balance,
                    monthlyContributionCents = contribution,
                    currency = currency,
                    childId = childId,
                    updatedAt = LocalDate.now()
                )
            )
                .onSuccess { _event.emit(SavingsEvent.Saved) }
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
        childId: Long?
    ) {
        val balance = balanceStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(SavingsEvent.Error("Montant invalide")) }
            return
        }
        val contribution = contributionStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            updateSavingsAccount(
                account.copy(
                    type                     = type,
                    label                    = label,
                    currentBalanceCents      = balance,
                    monthlyContributionCents = contribution,
                    currency                 = currency,
                    childId                  = childId,
                    updatedAt                = LocalDate.now()
                )
            )
                .onSuccess { _event.emit(SavingsEvent.Saved) }
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
                    updateSavingsAccount(
                        account.copy(
                            currentBalanceCents = account.currentBalanceCents + account.monthlyContributionCents,
                            updatedAt           = now
                        )
                    )
                    _event.emit(SavingsEvent.VersementApplique)
                }
                .onFailure { _event.emit(SavingsEvent.Error("Vérifier les informations saisies")) }
        }
    }
}

sealed class SavingsUiState {
    data object Loading : SavingsUiState()
    data class Success(
        val accounts: List<SavingsAccount>,
        val children: List<Child>
    ) : SavingsUiState()
    data class Error(val message: String) : SavingsUiState()
}

sealed class SavingsEvent {
    data object Saved : SavingsEvent()
    data object Deleted : SavingsEvent()
    data object ChildSaved : SavingsEvent()
    data object VersementApplique : SavingsEvent()
    data class Error(val message: String) : SavingsEvent()
}
