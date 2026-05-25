package com.dibitara.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.DuplicateGroup
import com.dibitara.app.domain.usecase.DeleteTransactionUseCase
import com.dibitara.app.domain.usecase.DetectDuplicateTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DuplicateCleanupUiState {
    data object Loading : DuplicateCleanupUiState
    data class Success(
        val groups: List<DuplicateGroup>,
        val suppressionEnCours: Boolean = false
    ) : DuplicateCleanupUiState
}

sealed interface DuplicateCleanupEvent {
    data object Supprime : DuplicateCleanupEvent
    data class Erreur(val message: String) : DuplicateCleanupEvent
}

@HiltViewModel
class DuplicateCleanupViewModel @Inject constructor(
    private val detectDoublons: DetectDuplicateTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DuplicateCleanupUiState>(DuplicateCleanupUiState.Loading)
    val uiState: StateFlow<DuplicateCleanupUiState> = _uiState.asStateFlow()

    private val _event = Channel<DuplicateCleanupEvent>()
    val event: Flow<DuplicateCleanupEvent> = _event.receiveAsFlow()

    init {
        viewModelScope.launch {
            detectDoublons().collect { groups ->
                // On préserve suppressionEnCours si une suppression est en cours
                // (Room peut émettre une mise à jour pendant que l'on supprime)
                val enCours = (_uiState.value as? DuplicateCleanupUiState.Success)?.suppressionEnCours ?: false
                _uiState.value = DuplicateCleanupUiState.Success(groups, enCours)
            }
        }
    }

    // Change la transaction à conserver dans un groupe (l'utilisateur peut inverser le choix par défaut)
    fun changerSelection(groupIndex: Int, keepId: Long) {
        val state = _uiState.value as? DuplicateCleanupUiState.Success ?: return
        val newGroups = state.groups.toMutableList()
        newGroups[groupIndex] = newGroups[groupIndex].copy(keepId = keepId)
        _uiState.value = state.copy(groups = newGroups)
    }

    fun supprimerDoublons() {
        val state = _uiState.value as? DuplicateCleanupUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(suppressionEnCours = true)
            val aSupprimer = state.groups.flatMap { group ->
                group.transactions.filter { it.id != group.keepId }
            }
            var avecErreur = false
            aSupprimer.forEach { transaction ->
                deleteTransaction(transaction).onFailure { avecErreur = true }
            }
            if (avecErreur) {
                _event.send(DuplicateCleanupEvent.Erreur("Certaines suppressions ont échoué"))
            } else {
                _event.send(DuplicateCleanupEvent.Supprime)
            }
        }
    }
}
