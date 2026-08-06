package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.UpcomingPayment
import com.dibitara.app.domain.usecase.GetCashflowProjectionUseCase
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetRecategorizationSuggestionsUseCase
import com.dibitara.app.domain.usecase.GetSpendingHistoryUseCase
import com.dibitara.app.domain.usecase.GetUpcomingPaymentsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateDashboardCardOrderUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
import com.dibitara.app.domain.usecase.UpsertCategorizationRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPatrimonyOverview   : GetPatrimonyOverviewUseCase,
    private val getSpendingHistory     : GetSpendingHistoryUseCase,
    private val getMonthlyReport       : GetMonthlyReportUseCase,
    private val getUpcomingPayments    : GetUpcomingPaymentsUseCase,
    private val getPreferences         : GetUserPreferencesUseCase,
    private val getCashflowProjection    : GetCashflowProjectionUseCase,
    private val getRecategorizations     : GetRecategorizationSuggestionsUseCase,
    private val getPatrimoineHistory     : GetPatrimoineHistoryUseCase,
    private val updateTransaction        : UpdateTransactionUseCase,
    private val updateCardOrder          : UpdateDashboardCardOrderUseCase,
    private val ucUpsertRule             : UpsertCategorizationRuleUseCase
) : ViewModel() {

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    fun toggleEditMode() { _isEditMode.value = !_isEditMode.value }

    /**
     * Déplace la carte identifiée par [fromKey] à la position de [toKey].
     * L'ordre courant est lu depuis [uiState] ; l'update est persisté dans DataStore.
     */
    fun moveCard(fromKey: String, toKey: String) {
        val currentOrder = (uiState.value as? DashboardUiState.Success)
            ?.cardOrder ?: return
        val from = currentOrder.indexOfFirst { it.name == fromKey }
        val to   = currentOrder.indexOfFirst { it.name == toKey }
        if (from < 0 || to < 0 || from == to) return
        val newOrder = currentOrder.toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch { updateCardOrder(newOrder) }
    }

    val uiState: StateFlow<DashboardUiState> = run {
        // Correction #5 : LocalDate.now() évalué à la création du ViewModel (non stocké),
        // ce qui garantit que le mois courant est frais si le ViewModel est recréé.
        val today = LocalDate.now()
        combine(
            getPatrimonyOverview(today.monthValue, today.year),
            getSpendingHistory(),
            getMonthlyReport(today.monthValue, today.year),
            getUpcomingPayments(limit = 5),
            getPreferences()
        ) { overview, history, rapport, upcoming, prefs ->
        // combine ne supporte que 5 arguments : les deux autres flows sont mergés séparément
        Quintuple(overview, history, rapport, upcoming, prefs)
    }.combine(
        getCashflowProjection()
    ) { q, cashflow -> q to cashflow }
    .combine(
        getRecategorizations()
    ) { (q, cashflow), recats -> Triple(q, cashflow, recats) }
    .combine(
        getPatrimoineHistory()
    ) { (q, cashflow, recats), history ->
        val prefs = q.fifth
        DashboardUiState.Success(
            overview                    = q.first,
            spendingHistory             = q.second,
            upcomingPayments            = if (prefs.afficherProchainsPaiements) q.fourth else emptyList(),
            rapportMensuel              = if (prefs.afficherRapportMensuel) q.third else null,
            cashflowProjection          = cashflow,
            recategorizationSuggestions = recats,
            patrimoineTrendPct          = calculerTendancePatrimoine(history),
            cardOrder                   = prefs.dashboardCardOrder
        ) as DashboardUiState
    }
        .catch { emit(DashboardUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )
    }

    /**
     * Applique la suggestion.
     * - Si [suggestedSubCategory] est non-null : reste dans AUTRE mais pose la sous-catégorie.
     *   La transaction disparaît des suggestions car subCategory != null.
     * - Sinon : change la catégorie principale et efface la sous-catégorie.
     *   La transaction disparaît des suggestions car elle n'est plus dans Category.AUTRE.
     */
    fun appliquerRecategorisation(suggestion: RecategorizationSuggestion) {
        viewModelScope.launch {
            val updated = if (suggestion.suggestedSubCategory != null) {
                suggestion.transaction.copy(subCategory = suggestion.suggestedSubCategory)
            } else {
                suggestion.transaction.copy(
                    category    = suggestion.suggestedCategory,
                    subCategory = null
                )
            }
            updateTransaction(updated)
            // Mémorise le choix pour que les prochaines occurrences du même marchand
            // récupèrent directement la bonne catégorie sans repasser par les suggestions.
            ucUpsertRule(
                note        = suggestion.transaction.note,
                type        = suggestion.transaction.type,
                category    = suggestion.suggestedCategory,
                subCategory = suggestion.suggestedSubCategory
            )
        }
    }

    /**
     * Refuse la suggestion : confirme que la transaction est bien "Autre / Divers".
     * Poser subCategory = DIVERS la sort définitivement des suggestions futures
     * sans changer sa catégorie principale - aucune migration Room requise.
     */
    fun refuserRecategorisation(suggestion: RecategorizationSuggestion) {
        viewModelScope.launch {
            updateTransaction(
                suggestion.transaction.copy(
                    subCategory = SubCategory.DIVERS
                )
            )
        }
    }

    // Tuple interne pour contourner la limite de 5 arguments de combine
    private data class Quintuple<A, B, C, D, E>(
        val first: A, val second: B, val third: C, val fourth: D, val fifth: E
    )
}

/**
 * Variation du patrimoine net (%) entre le plus ancien et le plus récent snapshot
 * disponibles - badge de tendance global du dashboard (refonte UX/UI 2026-08).
 * Un snapshot n'est enregistré que quand l'utilisateur visite l'écran Patrimoine :
 * l'historique peut donc être court ou absent. Retourne null tant qu'il n'y a pas
 * au moins 2 points, pour ne jamais afficher un pourcentage trompeur.
 */
private fun calculerTendancePatrimoine(history: List<PatrimoineSnapshot>): Float? {
    if (history.size < 2) return null
    val premier = history.first().patrimoineNetCents
    val dernier = history.last().patrimoineNetCents
    if (premier == 0L) return null
    return ((dernier - premier).toFloat() / premier.toFloat()) * 100f
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val overview                    : PatrimonyOverview,
        val spendingHistory             : List<MonthlyExpense>,
        val upcomingPayments            : List<UpcomingPayment>             = emptyList(),
        val rapportMensuel              : MonthlyReport?                    = null,
        val cashflowProjection          : CashflowProjection?               = null,
        val recategorizationSuggestions : List<RecategorizationSuggestion>  = emptyList(),
        val patrimoineTrendPct          : Float?                            = null,
        val cardOrder                   : List<DashboardCard>               = DashboardCard.entries.toList()
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
