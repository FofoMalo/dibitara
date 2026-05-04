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

    val uiState: StateFlow<BudgetUiState> = combine(
        getMonthlyBudget(now.monthValue, now.year),
        getMonthlyTransactions(now.monthValue, now.year)
    ) { budget, transactions ->
        BudgetUiState.Success(
            budget = budget,
            transactions = transactions,
            month = now.monthValue,
            year = now.year
        ) as BudgetUiState
    }
        .catch { emit(BudgetUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState.Loading
        )

    // Appelé quand l'utilisateur valide le formulaire de définition du budget
    fun saveBudget(amountEuros: String, currency: Currency) {
        val cents = amountEuros.toDoubleOrNull()?.let { (it * 100).toLong() } ?: return
        viewModelScope.launch {
            setBudget(
                Budget(
                    month = now.monthValue,
                    year = now.year,
                    allocatedCents = cents,
                    spentCents = currentSpentCents(),
                    currency = currency
                )
            )
        }
    }

    private fun currentSpentCents(): Long =
        (uiState.value as? BudgetUiState.Success)?.budget?.spentCents ?: 0L
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
