package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.UpcomingPayment
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetSpendingHistoryUseCase
import com.dibitara.app.domain.usecase.GetUpcomingPaymentsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase,
    private val getSpendingHistory: GetSpendingHistoryUseCase,
    private val getMonthlyReport: GetMonthlyReportUseCase,
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val getPreferences: GetUserPreferencesUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        getPatrimonyOverview(now.monthValue, now.year),
        getSpendingHistory(),
        getMonthlyReport(now.monthValue, now.year),
        getUpcomingPayments(limit = 5),
        getPreferences()
    ) { overview, history, rapport, upcoming, prefs ->
        DashboardUiState.Success(
            overview        = overview,
            spendingHistory = history,
            upcomingPayments = upcoming,
            // null si la fonctionnalité est désactivée — le Dashboard affiche alors le graphique
            rapportMensuel  = if (prefs.afficherRapportMensuel) rapport else null
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
        val spendingHistory: List<MonthlyExpense>,
        val upcomingPayments: List<UpcomingPayment> = emptyList(),
        val rapportMensuel: MonthlyReport? = null
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
