package com.dibitara.app.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.usecase.DeleteDebtUseCase
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.SaveDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val getDebts: GetDebtsUseCase,
    private val saveDebt: SaveDebtUseCase,
    private val deleteDebt: DeleteDebtUseCase
) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = getDebts()
        .map { debts -> DebtsUiState.Success(debts) as DebtsUiState }
        .catch { emit(DebtsUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DebtsUiState.Loading
        )

    private val _event = MutableSharedFlow<DebtsEvent>()
    val event: SharedFlow<DebtsEvent> = _event.asSharedFlow()

    fun addDebt(
        label: String,
        totalStr: String,
        monthlyStr: String,
        currency: Currency,
        type: DebtType
    ) {
        val totalCents = totalStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(DebtsEvent.Error("Montant total invalide")) }
            return
        }
        val monthlyCents = monthlyStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            saveDebt(
                Debt(
                    label = label,
                    totalCents = totalCents,
                    monthlyPaymentCents = monthlyCents,
                    currency = currency,
                    type = type,
                    updatedAt = LocalDate.now()
                )
            )
                .onSuccess { _event.emit(DebtsEvent.Saved) }
                .onFailure { _event.emit(DebtsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun removeDebt(debt: Debt) {
        viewModelScope.launch {
            deleteDebt(debt)
            _event.emit(DebtsEvent.Deleted)
        }
    }
}

sealed class DebtsUiState {
    data object Loading : DebtsUiState()
    data class Success(val debts: List<Debt>) : DebtsUiState()
    data class Error(val message: String) : DebtsUiState()
}

sealed class DebtsEvent {
    data object Saved : DebtsEvent()
    data object Deleted : DebtsEvent()
    data class Error(val message: String) : DebtsEvent()
}
