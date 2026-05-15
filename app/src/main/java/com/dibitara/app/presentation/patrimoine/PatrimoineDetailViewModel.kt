package com.dibitara.app.presentation.patrimoine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PatrimoineDetailViewModel @Inject constructor(
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<PatrimoineDetailUiState> =
        getPatrimonyOverview(now.monthValue, now.year)
            .map { PatrimoineDetailUiState.Success(it) as PatrimoineDetailUiState }
            .catch { emit(PatrimoineDetailUiState.Error(it.message ?: "Erreur inconnue")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PatrimoineDetailUiState.Loading
            )
}

sealed class PatrimoineDetailUiState {
    data object Loading : PatrimoineDetailUiState()
    data class Success(val overview: PatrimonyOverview) : PatrimoineDetailUiState()
    data class Error(val message: String) : PatrimoineDetailUiState()
}
