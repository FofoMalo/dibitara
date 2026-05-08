package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> =
        getPatrimonyOverview(now.monthValue, now.year)
            .map { overview -> DashboardUiState.Success(overview) as DashboardUiState }
            .catch { emit(DashboardUiState.Error(it.message ?: "Erreur inconnue")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState.Loading
            )
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val overview: PatrimonyOverview) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
