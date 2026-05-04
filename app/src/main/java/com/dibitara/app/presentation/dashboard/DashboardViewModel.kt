package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.usecase.GetMonthlyBudgetUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel du tableau de bord.
 *
 * Pattern utilisé : UiState scellé (sealed class) + StateFlow.
 * Le sealed class représente tous les états possibles de l'écran.
 * La UI observe ce Flow et se redessine automatiquement à chaque changement.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlyBudget: GetMonthlyBudgetUseCase,
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        getMonthlyBudget(now.monthValue, now.year),
        getMonthlyTransactions(now.monthValue, now.year)
    ) { budget, transactions ->
        DashboardUiState.Success(budget = budget, transactions = transactions)
    }
        .catch { emit(DashboardUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val budget: Budget?, val transactions: List<Transaction>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
