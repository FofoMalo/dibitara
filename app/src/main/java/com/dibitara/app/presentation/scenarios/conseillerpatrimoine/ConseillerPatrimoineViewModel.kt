package com.dibitara.app.presentation.scenarios.conseillerpatrimoine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.ConseilPatrimoineResult
import com.dibitara.app.domain.usecase.AnalyserPatrimoineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class ConseillerPatrimoineUiState {
    data object Loading : ConseillerPatrimoineUiState()
    data class Success(val conseil: ConseilPatrimoineResult) : ConseillerPatrimoineUiState()
}

@HiltViewModel
class ConseillerPatrimoineViewModel @Inject constructor(
    private val analyserPatrimoine: AnalyserPatrimoineUseCase
) : ViewModel() {

    val uiState: StateFlow<ConseillerPatrimoineUiState> = analyserPatrimoine()
        .map { ConseillerPatrimoineUiState.Success(it) }
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConseillerPatrimoineUiState.Loading
        )
}
