package com.dibitara.app.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val getMonthlyReport: GetMonthlyReportUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<MonthlyReportUiState> = getMonthlyReport(now.monthValue, now.year)
        .map  { MonthlyReportUiState.Success(it) as MonthlyReportUiState }
        .catch { emit(MonthlyReportUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyReportUiState.Loading
        )
}

sealed class MonthlyReportUiState {
    data object Loading : MonthlyReportUiState()
    data class Success(val report: MonthlyReport) : MonthlyReportUiState()
    data class Error(val message: String) : MonthlyReportUiState()
}
