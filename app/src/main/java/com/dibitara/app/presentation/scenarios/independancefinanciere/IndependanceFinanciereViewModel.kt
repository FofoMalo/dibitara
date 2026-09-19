package com.dibitara.app.presentation.scenarios.independancefinanciere

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CapIndependanceFinanciere
import com.dibitara.app.domain.model.PocheRecommandee
import com.dibitara.app.domain.usecase.GetCapIndependanceFinanciereUseCase
import com.dibitara.app.domain.usecase.GetSpendingRecommendationsUseCase
import com.dibitara.app.domain.usecase.UpdateMultipleFICibleUseCase
import com.dibitara.app.domain.usecase.UpdateRendementFIEspereUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class IndependanceFinanciereUiState {
    data object Loading : IndependanceFinanciereUiState()
    /** [cap] est null tant que l'écran Patrimoine n'a jamais été ouvert (aucun snapshot). */
    data class Success(val cap: CapIndependanceFinanciere?, val poches: List<PocheRecommandee>) : IndependanceFinanciereUiState()
}

@HiltViewModel
class IndependanceFinanciereViewModel @Inject constructor(
    private val getCapIndependanceFinanciere: GetCapIndependanceFinanciereUseCase,
    private val getSpendingRecommendations  : GetSpendingRecommendationsUseCase,
    private val updateMultipleFICible       : UpdateMultipleFICibleUseCase,
    private val updateRendementFIEspere     : UpdateRendementFIEspereUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<IndependanceFinanciereUiState> = combine(
        getCapIndependanceFinanciere(now.monthValue, now.year),
        getSpendingRecommendations(now.monthValue, now.year)
    ) { cap, recommandation ->
        IndependanceFinanciereUiState.Success(cap, recommandation.pouchesRecommandees)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IndependanceFinanciereUiState.Loading
    )

    fun mettreAJourMultiple(multiple: Int) {
        viewModelScope.launch { updateMultipleFICible(multiple.coerceIn(15, 40)) }
    }

    fun mettreAJourRendement(pct: Int) {
        viewModelScope.launch { updateRendementFIEspere(pct.coerceIn(0, 15)) }
    }
}
