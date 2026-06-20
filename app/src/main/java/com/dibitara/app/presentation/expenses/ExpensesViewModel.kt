package com.dibitara.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionSuggestion
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.AddTransactionUseCase
import com.dibitara.app.domain.usecase.DeleteCustomSubCategoryUseCase
import com.dibitara.app.domain.usecase.DeleteTransactionUseCase
import com.dibitara.app.domain.usecase.GetAllTransactionsUseCase
import com.dibitara.app.domain.usecase.GetCustomSubCategoriesUseCase
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase
import com.dibitara.app.domain.usecase.GetTransactionSuggestionsUseCase
import com.dibitara.app.domain.usecase.GetTransactionsByDateRangeUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
import com.dibitara.app.domain.usecase.UpsertCategorizationRuleUseCase
import com.dibitara.app.domain.usecase.UpsertCustomSubCategoryUseCase
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val ucGetMonthlyTransactions : GetMonthlyTransactionsUseCase,
    private val ucGetByDateRange         : GetTransactionsByDateRangeUseCase,
    private val ucGetAll                 : GetAllTransactionsUseCase,
    private val ucAdd                    : AddTransactionUseCase,
    private val ucUpdate                 : UpdateTransactionUseCase,
    private val ucDelete                 : DeleteTransactionUseCase,
    private val ucGetCustomSubCategories : GetCustomSubCategoriesUseCase,
    private val ucUpsertCustomSubCategory: UpsertCustomSubCategoryUseCase,
    private val ucDeleteCustomSubCategory: DeleteCustomSubCategoryUseCase,
    private val ucGetPreferences         : GetUserPreferencesUseCase,
    private val ucGetSuggestions         : GetTransactionSuggestionsUseCase,
    private val ucUpsertRule             : UpsertCategorizationRuleUseCase,
    savedStateHandle                     : SavedStateHandle
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    // Suggestions de saisie rapide - issues des 30 derniers jours, fréquence ≥ 2
    val suggestions: StateFlow<List<TransactionSuggestion>> = ucGetSuggestions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Mois initial : depuis les args de navigation si on arrive de BudgetScreen, sinon mois courant
    private val now = LocalDate.now()
    private val _selectedMonth = MutableStateFlow(
        savedStateHandle.get<String>("month")?.toIntOrNull() ?: now.monthValue
    )
    private val _selectedYear  = MutableStateFlow(
        savedStateHandle.get<String>("year")?.toIntOrNull() ?: now.year
    )

    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()
    val selectedYear:  StateFlow<Int> = _selectedYear.asStateFlow()

    // Filtre initial : pré-rempli si on arrive depuis BudgetScreen via navigation avec args
    private val _filter = MutableStateFlow(
        ExpensesFilter(
            category = savedStateHandle.get<String>("category")
                ?.let { runCatching { Category.valueOf(it) }.getOrNull() },
            transactionType = savedStateHandle.get<String>("type")
                ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                ?: TransactionType.EXPENSE
        )
    )
    val filter: StateFlow<ExpensesFilter> = _filter.asStateFlow()

    /**
     * L'UI observe ce flow pour afficher la liste.
     *
     * Le filtre de date est poussé au niveau SQL selon la [FilterPeriod] :
     * - CURRENT_MONTH → [GetMonthlyTransactionsUseCase] sur le mois sélectionné
     * - THREE_MONTHS / SIX_MONTHS → [GetTransactionsByDateRangeUseCase]
     * - ALL → [GetAllTransactionsUseCase] (chargement complet - à utiliser avec parcimonie)
     *
     * Les critères restants (catégorie, type, recherche, tri) sont appliqués en mémoire
     * sur le sous-ensemble déjà filtré par la base de données.
     */
    val uiState: StateFlow<ExpensesUiState> = combine(
        _filter,
        _selectedMonth,
        _selectedYear
    ) { filter, month, year -> Triple(filter, month, year) }
    .flatMapLatest { (filter, month, year) ->
        val today = LocalDate.now()
        val transactionsFlow = when (filter.period) {
            FilterPeriod.CURRENT_MONTH -> ucGetMonthlyTransactions(month, year)
            FilterPeriod.THREE_MONTHS  -> ucGetByDateRange(
                today.withDayOfMonth(1).minusMonths(2), today
            )
            FilterPeriod.SIX_MONTHS   -> ucGetByDateRange(
                today.withDayOfMonth(1).minusMonths(5), today
            )
            FilterPeriod.ALL           -> ucGetAll()
        }
        combine(transactionsFlow, ucGetCustomSubCategories()) { transactions, customSubCats ->
            ExpensesUiState.Success(
                expenses            = filter.apply(transactions),
                customSubCategories = customSubCats
            ) as ExpensesUiState
        }
    }
    .catch { emit(ExpensesUiState.Error(it.message ?: "Erreur inconnue")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState.Loading
    )

    private val _event = MutableSharedFlow<ExpensesEvent>()
    val event: SharedFlow<ExpensesEvent> = _event.asSharedFlow()

    fun updateFilter(filter: ExpensesFilter) {
        // Revenir au mois courant quand l'utilisateur resélectionne "Ce mois" depuis une autre période
        if (filter.period == FilterPeriod.CURRENT_MONTH && _filter.value.period != FilterPeriod.CURRENT_MONTH) {
            val today = LocalDate.now()
            _selectedMonth.value = today.monthValue
            _selectedYear.value  = today.year
        }
        _filter.value = filter
    }

    // Navigation mensuelle - uniquement pertinente quand period == CURRENT_MONTH
    fun previousMonth() {
        val current = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1).minusMonths(1)
        _selectedMonth.value = current.monthValue
        _selectedYear.value  = current.year
    }

    fun nextMonth() {
        val current = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1).plusMonths(1)
        // Bloquer au mois courant pour ne pas afficher un mois futur vide
        val today = LocalDate.now()
        if (current.year < today.year || (current.year == today.year && current.monthValue <= today.monthValue)) {
            _selectedMonth.value = current.monthValue
            _selectedYear.value  = current.year
        }
    }

    fun addExpense(
        amountStr: String,
        category: Category,
        currency: Currency,
        note: String,
        date: LocalDate = LocalDate.now(),
        type: TransactionType = TransactionType.EXPENSE,
        childId: Long? = null,
        isRecurring: Boolean = false,
        recurrenceDay: Int? = null,
        subCategory: SubCategory? = null,
        customSubCategoryId: Long? = null,
        recurrenceFrequency: RecurrenceFrequency? = null,
        endDate: LocalDate? = null
    ) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(ExpensesEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucAdd(
                Transaction(
                    amountCents = cents,
                    currency = currency,
                    category = category,
                    type = type,
                    date = date,
                    note = note,
                    childId = childId,
                    isRecurring = isRecurring,
                    recurrenceDay = recurrenceDay,
                    subCategory = subCategory,
                    customSubCategoryId = customSubCategoryId,
                    recurrenceFrequency = recurrenceFrequency,
                    firstPaymentDate = if (isRecurring) date else null,
                    endDate = endDate
                )
            )
                .onSuccess {
                    ucUpsertRule(note, type, category, subCategory, customSubCategoryId)
                    _event.emit(ExpensesEvent.Saved)
                }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateExpense(
        original: Transaction,
        amountStr: String,
        category: Category,
        currency: Currency,
        note: String,
        date: LocalDate,
        type: TransactionType,
        childId: Long? = null,
        isRecurring: Boolean = false,
        recurrenceDay: Int? = null,
        subCategory: SubCategory? = null,
        customSubCategoryId: Long? = null,
        recurrenceFrequency: RecurrenceFrequency? = null,
        endDate: LocalDate? = null
    ) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(ExpensesEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdate(
                original.copy(
                    amountCents = cents,
                    currency = currency,
                    category = category,
                    type = type,
                    date = date,
                    note = note,
                    childId = childId,
                    isRecurring = isRecurring,
                    recurrenceDay = recurrenceDay,
                    subCategory = subCategory,
                    customSubCategoryId = customSubCategoryId,
                    recurrenceFrequency = recurrenceFrequency,
                    firstPaymentDate = original.firstPaymentDate ?: if (isRecurring) date else null,
                    endDate = endDate
                )
            )
                .onSuccess {
                    ucUpsertRule(note, type, category, subCategory, customSubCategoryId)
                    _event.emit(ExpensesEvent.Saved)
                    // Si la catégorie a changé et la note est identifiable, proposer de tout recatégoriser
                    if (category != original.category && note.isNotBlank()) {
                        val autres = ucGetAll().first()
                            .filter { it.id != original.id && it.note.trim() == note.trim() && it.category != category }
                        if (autres.isNotEmpty()) {
                            _event.emit(ExpensesEvent.RecategorizationProposee(autres.size, note, category))
                        }
                    }
                }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }

    /** Applique [newCategory] à toutes les transactions ayant exactement la même note. */
    fun recategoriserParNote(note: String, newCategory: Category) {
        viewModelScope.launch {
            val aModifier = ucGetAll().first()
                .filter { it.note.trim() == note.trim() && it.category != newCategory }
            aModifier.forEach { ucUpdate(it.copy(category = newCategory)) }
            _event.emit(ExpensesEvent.RecategorizationTerminee(aModifier.size))
        }
    }

    // ─── Gestion des sous-catégories personnalisées ───────────────────────────

    fun creerCustomSubCategory(name: String, category: Category) {
        viewModelScope.launch {
            ucUpsertCustomSubCategory(CustomSubCategory(name = name, parentCategory = category))
        }
    }

    fun renommerCustomSubCategory(subCategory: CustomSubCategory, newName: String) {
        viewModelScope.launch {
            ucUpsertCustomSubCategory(subCategory.copy(name = newName))
        }
    }

    fun supprimerCustomSubCategory(subCategory: CustomSubCategory) {
        viewModelScope.launch { ucDeleteCustomSubCategory(subCategory) }
    }

    fun deleteExpense(transaction: Transaction) {
        viewModelScope.launch {
            ucDelete(transaction)
                .onSuccess { _event.emit(ExpensesEvent.Deleted) }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
        }
    }
}

// ─── Modèle de filtre ────────────────────────────────────────────────────────

/**
 * Regroupe tous les critères de filtrage et de tri non-temporels.
 * La période ([period]) détermine quelle requête SQL est exécutée dans le ViewModel ;
 * [apply] ne filtre que la catégorie, le type, la recherche textuelle et le tri.
 */
data class ExpensesFilter(
    val query           : String              = "",
    val category        : Category?           = null,
    val period          : FilterPeriod        = FilterPeriod.CURRENT_MONTH,
    val transactionType : TransactionType?    = TransactionType.EXPENSE,
    val sort            : SortOrder           = SortOrder.DATE_DESC
) {
    fun apply(transactions: List<Transaction>): List<Transaction> =
        transactions
            .filter { transactionType == null || it.type == transactionType }
            .filter { category == null || it.category == category }
            .filter { query.isBlank() || it.note.contains(query, ignoreCase = true) }
            .let { list ->
                when (sort) {
                    SortOrder.DATE_DESC   -> list.sortedByDescending { it.date }
                    SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amountCents }
                }
            }
}

enum class FilterPeriod(val label: String) {
    CURRENT_MONTH("Ce mois"),
    THREE_MONTHS("3 mois"),
    SIX_MONTHS("6 mois"),
    ALL("Tout")
}

enum class SortOrder(val label: String) {
    DATE_DESC("Date ↓"),
    AMOUNT_DESC("Montant ↓")
}

// ─── États UI ────────────────────────────────────────────────────────────────

sealed class ExpensesUiState {
    data object Loading : ExpensesUiState()
    data class Success(
        val expenses            : List<Transaction>,
        val customSubCategories : List<CustomSubCategory> = emptyList()
    ) : ExpensesUiState()
    data class Error(val message: String) : ExpensesUiState()
}

sealed class ExpensesEvent {
    data object Saved   : ExpensesEvent()
    data object Deleted : ExpensesEvent()
    data class Error(val message: String) : ExpensesEvent()
    /** Émis après updateExpense quand d'autres transactions partagent la même note. */
    data class RecategorizationProposee(
        val count      : Int,
        val note       : String,
        val newCategory: Category
    ) : ExpensesEvent()
    /** Émis après recategoriserParNote pour afficher un snackbar de confirmation. */
    data class RecategorizationTerminee(val count: Int) : ExpensesEvent()
}
