package com.dibitara.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.AddTransactionUseCase
import com.dibitara.app.domain.usecase.DeleteTransactionUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<ExpensesUiState> =
        getMonthlyTransactions(now.monthValue, now.year)
            .map { transactions ->
                ExpensesUiState.Success(
                    expenses = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sortedByDescending { it.date }
                ) as ExpensesUiState
            }
            .catch { emit(ExpensesUiState.Error(it.message ?: "Erreur inconnue")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpensesUiState.Loading
            )

    private val _event = MutableSharedFlow<ExpensesEvent>()
    val event: SharedFlow<ExpensesEvent> = _event.asSharedFlow()

    fun addExpense(
        amountStr: String,
        category: Category,
        currency: Currency,
        note: String,
        childId: Long? = null,
        isRecurring: Boolean = false,
        recurrenceDay: Int? = null
    ) {
        val cents = amountStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(ExpensesEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            addTransaction(
                Transaction(
                    amountCents = cents,
                    currency = currency,
                    category = category,
                    type = TransactionType.EXPENSE,
                    date = now,
                    note = note,
                    childId = childId,
                    isRecurring = isRecurring,
                    recurrenceDay = recurrenceDay
                )
            )
                .onSuccess { _event.emit(ExpensesEvent.Saved) }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateExpense(
        original: Transaction,
        amountStr: String,
        category: Category,
        currency: Currency,
        note: String,
        childId: Long? = null,
        isRecurring: Boolean = false,
        recurrenceDay: Int? = null
    ) {
        val cents = amountStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(ExpensesEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            updateTransaction(
                original.copy(
                    amountCents = cents,
                    currency = currency,
                    category = category,
                    note = note,
                    childId = childId,
                    isRecurring = isRecurring,
                    recurrenceDay = recurrenceDay
                )
            )
                .onSuccess { _event.emit(ExpensesEvent.Saved) }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteExpense(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransaction(transaction)
                .onSuccess { _event.emit(ExpensesEvent.Deleted) }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }
}

sealed class ExpensesUiState {
    data object Loading : ExpensesUiState()
    data class Success(val expenses: List<Transaction>) : ExpensesUiState()
    data class Error(val message: String) : ExpensesUiState()
}

sealed class ExpensesEvent {
    data object Saved : ExpensesEvent()
    data object Deleted : ExpensesEvent()
    data class Error(val message: String) : ExpensesEvent()
}
