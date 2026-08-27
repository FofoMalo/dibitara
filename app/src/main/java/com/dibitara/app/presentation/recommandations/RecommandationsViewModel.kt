package com.dibitara.app.presentation.recommandations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.PocheRecommandee
import com.dibitara.app.domain.model.SpendingRecommendation
import com.dibitara.app.domain.usecase.GetSpendingRecommendationsUseCase
import com.dibitara.app.domain.usecase.UpdateTauxEpargneCibleUseCase
import com.dibitara.app.domain.usecase.UpsertCategoryEnvelopeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** État possible de l'écran de recommandations. */
sealed class RecommandationsUiState {
    data object Loading : RecommandationsUiState()
    data class Success(val recommandation: SpendingRecommendation) : RecommandationsUiState()
}

@HiltViewModel
class RecommandationsViewModel @Inject constructor(
    private val getRecommandations    : GetSpendingRecommendationsUseCase,
    private val upsertEnveloppe       : UpsertCategoryEnvelopeUseCase,
    private val updateTauxEpargneCible: UpdateTauxEpargneCibleUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    /**
     * Recommandations calculées pour le mois courant.
     * Le Flow se recalcule automatiquement si les transactions, dettes ou comptes changent.
     */
    val uiState: StateFlow<RecommandationsUiState> = getRecommandations(now.monthValue, now.year)
        .map { RecommandationsUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecommandationsUiState.Loading
        )

    /**
     * Applique la recommandation d'une poche en créant ou mettant à jour
     * l'enveloppe budgétaire correspondante (réutilise le mécanisme du Sprint 40).
     */
    fun appliquerPoche(poche: PocheRecommandee) {
        viewModelScope.launch {
            val enveloppe = CategoryEnvelope(
                // Conserve l'id existant si une enveloppe est déjà configurée
                id           = poche.enveloppeExistante?.id ?: 0,
                category     = poche.category,
                plafondCents = poche.recommandeCents,
                currency     = (uiState.value as? RecommandationsUiState.Success)
                    ?.recommandation?.currency
                    ?: poche.enveloppeExistante?.currency
                    ?: com.dibitara.app.domain.model.Currency.EUR
            )
            upsertEnveloppe(enveloppe)
        }
    }

    /** Met à jour l'objectif d'épargne cible (stocké dans DataStore, sans migration Room). */
    fun mettreAJourTauxEpargneCible(pct: Int) {
        viewModelScope.launch {
            updateTauxEpargneCible(pct.coerceIn(0, 100))
        }
    }
}
