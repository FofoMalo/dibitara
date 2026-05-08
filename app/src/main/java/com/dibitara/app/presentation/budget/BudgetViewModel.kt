package com.dibitara.app.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.usecase.GetMonthlyBudgetUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import com.dibitara.app.domain.usecase.SetBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getMonthlyBudget: GetMonthlyBudgetUseCase,
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val setBudget: SetBudgetUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    // Mois sélectionné — l'utilisateur peut naviguer dans le temps
    private val _selectedMonth = MutableStateFlow(now.monthValue)
    private val _selectedYear  = MutableStateFlow(now.year)

    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()
    val selectedYear:  StateFlow<Int> = _selectedYear.asStateFlow()

    val uiState: StateFlow<BudgetUiState> = combine(
        _selectedMonth,
        _selectedYear
    ) { month, year -> Pair(month, year) }
        .flatMapLatest { (month, year) ->
            combine(
                getMonthlyBudget(month, year),
                getMonthlyTransactions(month, year)
            ) { budget, transactions ->
                BudgetUiState.Success(
                    budget = budget,
                    transactions = transactions,
                    month = month,
                    year = year
                ) as BudgetUiState
            }
        }
        .catch { emit(BudgetUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState.Loading
        )

    fun previousMonth() {
        val current = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1).minusMonths(1)
        _selectedMonth.value = current.monthValue
        _selectedYear.value  = current.year
    }

    fun nextMonth() {
        val current = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1).plusMonths(1)
        _selectedMonth.value = current.monthValue
        _selectedYear.value  = current.year
    }

    fun saveBudget(amountEuros: String, currency: Currency) {
        val cents = amountEuros.toDoubleOrNull()?.let { (it * 100).toLong() } ?: return
        val month = _selectedMonth.value
        val year  = _selectedYear.value
        viewModelScope.launch {
            setBudget(
                Budget(
                    month = month,
                    year  = year,
                    allocatedCents = cents,
                    spentCents = (uiState.value as? BudgetUiState.Success)?.budget?.spentCents ?: 0L,
                    currency = currency
                )
            )
        }
    }
}

sealed class BudgetUiState {
    data object Loading : BudgetUiState()
    data class Success(
        val budget: Budget?,
        val transactions: List<Transaction>,
        val month: Int,
        val year: Int
    ) : BudgetUiState()
    data class Error(val message: String) : BudgetUiState()
}
