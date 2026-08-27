package com.dibitara.app.presentation.projection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.usecase.GetCashflowProjectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProjectionDetailViewModel @Inject constructor(
    private val getCashflowProjection: GetCashflowProjectionUseCase
) : ViewModel() {

    val projection: StateFlow<CashflowProjection?> = getCashflowProjection()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
