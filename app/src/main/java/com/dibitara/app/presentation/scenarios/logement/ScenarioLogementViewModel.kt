package com.dibitara.app.presentation.scenarios.logement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CapaciteLogementResult
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScenarioLogementProjection
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.SimulerCapaciteLogementUseCase
import com.dibitara.app.domain.usecase.SimulerProjectionLogementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Une source de revenu simulée (ex : "Toi", "Conjoint·e") - purement une saisie d'écran, jamais persistée. */
data class LigneRevenu(val id: Int, val label: String, val montantCents: Long)

sealed class ScenarioLogementUiState {
    data object Loading : ScenarioLogementUiState()
    /** Aucun bien immobilier enregistré - rien à simuler. */
    data object AucunBien : ScenarioLogementUiState()
    data class Success(
        val biens          : List<RealEstateAsset>,
        val bienSelectionne: RealEstateAsset,
        val lignesRevenu    : List<LigneRevenu>,
        val capacite        : CapaciteLogementResult,
        val projection       : ScenarioLogementProjection
    ) : ScenarioLogementUiState()
}

private const val HORIZON_PROJECTION_MOIS = 6

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScenarioLogementViewModel @Inject constructor(
    private val getRealEstate      : GetRealEstateUseCase,
    private val simulerCapacite    : SimulerCapaciteLogementUseCase,
    private val simulerProjection  : SimulerProjectionLogementUseCase
) : ViewModel() {

    private val bienSelectionneId = MutableStateFlow<Long?>(null)
    private val lignesRevenu = MutableStateFlow(
        listOf(
            LigneRevenu(id = 0, label = "Toi", montantCents = 0L),
            LigneRevenu(id = 1, label = "Conjoint·e", montantCents = 0L)
        )
    )
    private var prochainIdLigne = 2

    val uiState: StateFlow<ScenarioLogementUiState> = combine(
        getRealEstate(), bienSelectionneId, lignesRevenu
    ) { biens, selectionId, lignes -> Triple(biens, selectionId, lignes) }
        .flatMapLatest { (biens, selectionId, lignes) ->
            if (biens.isEmpty()) return@flatMapLatest flowOf(ScenarioLogementUiState.AucunBien)

            val bien = biens.firstOrNull { it.id == selectionId } ?: biens.first()
            val revenuSimuleCents = lignes.sumOf { it.montantCents }

            simulerCapacite(bien.id, revenuSimuleCents).flatMapLatest { capacite ->
                if (capacite == null) {
                    flowOf(ScenarioLogementUiState.AucunBien)
                } else {
                    simulerProjection(
                        maisonDebtId                = bien.debtId,
                        revenuSimuleCents           = revenuSimuleCents,
                        besoinsIncompressiblesCents = capacite.besoinsIncompressiblesCents,
                        horizonMois                 = HORIZON_PROJECTION_MOIS
                    ).map { projection ->
                        ScenarioLogementUiState.Success(
                            biens           = biens,
                            bienSelectionne = bien,
                            lignesRevenu     = lignes,
                            capacite         = capacite,
                            projection       = projection
                        )
                    }
                }
            }
        }
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScenarioLogementUiState.Loading
        )

    fun selectionnerBien(id: Long) {
        bienSelectionneId.value = id
    }

    fun mettreAJourLigneRevenu(id: Int, montantCents: Long) {
        lignesRevenu.value = lignesRevenu.value.map {
            if (it.id == id) it.copy(montantCents = montantCents) else it
        }
    }

    fun ajouterLigneRevenu() {
        val nouvelId = prochainIdLigne++
        lignesRevenu.value = lignesRevenu.value + LigneRevenu(nouvelId, "Autre revenu", 0L)
    }
}
