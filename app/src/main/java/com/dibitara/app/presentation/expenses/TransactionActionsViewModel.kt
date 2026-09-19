package com.dibitara.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.usecase.CategoriserSelectionUseCase
import com.dibitara.app.domain.usecase.ObserverCorbeilleUseCase
import com.dibitara.app.domain.usecase.RestaurerTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionActionsViewModel @Inject constructor(
    observer: ObserverCorbeilleUseCase,
    private val restaurer: RestaurerTransactionUseCase,
    private val categoriser: CategoriserSelectionUseCase
) : ViewModel() {
    val corbeille = observer().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun effacerMessage() { _message.value = null }
    fun restaurer(id: Long) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try { restaurer.invoke(id).fold(
                onSuccess = { _message.value = "Transaction restaurée" },
                onFailure = { _message.value = it.message ?: "Restauration impossible" }
            ) } finally { _busy.value = false }
        }
    }
    fun categoriser(ids: Set<Long>, category: Category, onSuccess: () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try { categoriser.invoke(ids, category).fold(
                onSuccess = { _message.value = "$it dépenses catégorisées"; onSuccess() },
                onFailure = { _message.value = it.message ?: "Catégorisation impossible" }
            ) } finally { _busy.value = false }
        }
    }
}
