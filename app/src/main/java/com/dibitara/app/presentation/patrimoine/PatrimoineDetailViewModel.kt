package com.dibitara.app.presentation.patrimoine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.SavePatrimoineSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PatrimoineDetailViewModel @Inject constructor(
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase,
    private val getHistory: GetPatrimoineHistoryUseCase,
    private val saveSnapshot: SavePatrimoineSnapshotUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<PatrimoineDetailUiState> =
        combine(
            getPatrimonyOverview(now.monthValue, now.year),
            getHistory()
        ) { overview, history ->
            PatrimoineDetailUiState.Success(overview, history) as PatrimoineDetailUiState
        }
        .catch { emit(PatrimoineDetailUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PatrimoineDetailUiState.Loading
        )

    init {
        // Enregistre un snapshot du jour en arrière-plan dès le premier chargement
        viewModelScope.launch {
            getPatrimonyOverview(now.monthValue, now.year)
                .first()
                .let { overview -> saveSnapshot(overview) }
        }
    }
}

sealed class PatrimoineDetailUiState {
    data object Loading : PatrimoineDetailUiState()
    data class Success(
        val overview: PatrimonyOverview,
        val history: List<PatrimoineSnapshot> = emptyList()
    ) : PatrimoineDetailUiState()
    data class Error(val message: String) : PatrimoineDetailUiState()
}
