package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetSpendingHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase,
    private val getSpendingHistory: GetSpendingHistoryUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        getPatrimonyOverview(now.monthValue, now.year),
        getSpendingHistory()
    ) { overview, spendingHistory ->
        DashboardUiState.Success(
            overview = overview,
            spendingHistory = spendingHistory
        ) as DashboardUiState
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
    data class Success(
        val overview: PatrimonyOverview,
        val spendingHistory: List<MonthlyExpense>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
