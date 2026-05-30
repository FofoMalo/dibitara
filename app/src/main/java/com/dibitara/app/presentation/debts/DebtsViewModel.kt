package com.dibitara.app.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.usecase.DeleteDebtUseCase
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
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
    private val deleteDebt: DeleteDebtUseCase,
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    val uiState: StateFlow<DebtsUiState> = combine(
        getDebts(),
        ucGetPreferences(),
        exchangeRateRepository.getRatesFlow()
    ) { debts, prefs, rates ->
        // Conversion de chaque dette vers la devise par défaut avant sommation
        val target = prefs.deviseParDefaut
        val total   = debts.sumOf { CurrencyConverter.convertCents(it.totalCents, it.currency, target, rates) }
        val monthly = debts.sumOf { CurrencyConverter.convertCents(it.monthlyPaymentCents, it.currency, target, rates) }
        DebtsUiState.Success(
            debts             = debts,
            totalCents        = total,
            totalMonthlyCents = monthly,
            summaryCurrency   = target
        ) as DebtsUiState
    }
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
        originalStr: String = "",
        paymentDay: Int? = null,
        currency: Currency,
        type: DebtType
    ) {
        val totalCents = totalStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(DebtsEvent.Error("Montant total invalide")) }
            return
        }
        val monthlyCents = monthlyStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val originalCents = originalStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            saveDebt(
                Debt(
                    label               = label,
                    totalCents          = totalCents,
                    monthlyPaymentCents = monthlyCents,
                    originalAmountCents = originalCents,
                    paymentDay          = paymentDay,
                    currency            = currency,
                    type                = type,
                    updatedAt           = LocalDate.now()
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

    fun editDebt(
        debt: Debt,
        label: String,
        totalStr: String,
        monthlyStr: String,
        originalStr: String,
        paymentDay: Int?,
        currency: Currency,
        type: DebtType
    ) {
        val totalCents = totalStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: return
        val monthlyCents = monthlyStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val originalCents = originalStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            saveDebt(
                debt.copy(
                    label               = label,
                    totalCents          = totalCents,
                    monthlyPaymentCents = monthlyCents,
                    originalAmountCents = originalCents,
                    paymentDay          = paymentDay,
                    currency            = currency,
                    type                = type,
                    updatedAt           = LocalDate.now()
                )
            )
                .onSuccess { _event.emit(DebtsEvent.Saved) }
                .onFailure { _event.emit(DebtsEvent.Error(it.message ?: "Erreur")) }
        }
    }
}

sealed class DebtsUiState {
    data object Loading : DebtsUiState()
    data class Success(
        val debts             : List<Debt>,
        val totalCents        : Long     = 0L,
        val totalMonthlyCents : Long     = 0L,
        val summaryCurrency   : Currency = Currency.EUR
    ) : DebtsUiState()
    data class Error(val message: String) : DebtsUiState()
}

sealed class DebtsEvent {
    data object Saved : DebtsEvent()
    data object Deleted : DebtsEvent()
    data class Error(val message: String) : DebtsEvent()
}
