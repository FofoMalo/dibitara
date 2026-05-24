package com.dibitara.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.UpcomingPayment
import com.dibitara.app.domain.usecase.GetCashflowProjectionUseCase
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetRecategorizationSuggestionsUseCase
import com.dibitara.app.domain.usecase.GetSpendingHistoryUseCase
import com.dibitara.app.domain.usecase.GetUpcomingPaymentsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
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
    private val getCashflowProjection  : GetCashflowProjectionUseCase,
    private val getRecategorizations   : GetRecategorizationSuggestionsUseCase,
    private val updateTransaction      : UpdateTransactionUseCase
) : ViewModel() {

    private val now = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        getPatrimonyOverview(now.monthValue, now.year),
        getSpendingHistory(),
        getMonthlyReport(now.monthValue, now.year),
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
    ) { (q, cashflow), recats ->
        val prefs = q.fifth
        DashboardUiState.Success(
            overview                  = q.first,
            spendingHistory           = q.second,
            upcomingPayments          = if (prefs.afficherProchainsPaiements) q.fourth else emptyList(),
            rapportMensuel            = if (prefs.afficherRapportMensuel) q.third else null,
            cashflowProjection        = cashflow,
            recategorizationSuggestions = recats
        ) as DashboardUiState
    }
        .catch { emit(DashboardUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )

    /**
     * Applique la suggestion : change la catégorie et efface la sous-catégorie.
     * La transaction disparaît des suggestions car elle n'est plus dans Category.AUTRE.
     */
    fun appliquerRecategorisation(suggestion: RecategorizationSuggestion) {
        viewModelScope.launch {
            updateTransaction(
                suggestion.transaction.copy(
                    category    = suggestion.suggestedCategory,
                    subCategory = null
                )
            )
        }
    }

    /**
     * Refuse la suggestion : confirme que la transaction est bien "Autre / Divers".
     * Poser subCategory = DIVERS la sort définitivement des suggestions futures
     * sans changer sa catégorie principale — aucune migration Room requise.
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

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val overview                    : PatrimonyOverview,
        val spendingHistory             : List<MonthlyExpense>,
        val upcomingPayments            : List<UpcomingPayment>             = emptyList(),
        val rapportMensuel              : MonthlyReport?                    = null,
        val cashflowProjection          : CashflowProjection?               = null,
        val recategorizationSuggestions : List<RecategorizationSuggestion>  = emptyList()
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
