package com.dibitara.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.usecase.AddTransactionUseCase
import com.dibitara.app.domain.usecase.DeleteCustomSubCategoryUseCase
import com.dibitara.app.domain.usecase.DeleteTransactionUseCase
import com.dibitara.app.domain.usecase.GetAllTransactionsUseCase
import com.dibitara.app.domain.usecase.GetCustomSubCategoriesUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
import com.dibitara.app.domain.usecase.UpsertCustomSubCategoryUseCase
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val ucGetAll: GetAllTransactionsUseCase,
    private val ucAdd: AddTransactionUseCase,
    private val ucUpdate: UpdateTransactionUseCase,
    private val ucDelete: DeleteTransactionUseCase,
    private val ucGetCustomSubCategories: GetCustomSubCategoriesUseCase,
    private val ucUpsertCustomSubCategory: UpsertCustomSubCategoryUseCase,
    private val ucDeleteCustomSubCategory: DeleteCustomSubCategoryUseCase,
    private val ucGetPreferences: GetUserPreferencesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

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

    val uiState: StateFlow<ExpensesUiState> = combine(
        ucGetAll(),
        _filter,
        ucGetCustomSubCategories()
    ) { transactions, filter, customSubCats ->
        ExpensesUiState.Success(
            expenses            = filter.apply(transactions),
            customSubCategories = customSubCats
        ) as ExpensesUiState
    }
        .catch { emit(ExpensesUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpensesUiState.Loading
        )

    private val _event = MutableSharedFlow<ExpensesEvent>()
    val event: SharedFlow<ExpensesEvent> = _event.asSharedFlow()

    fun updateFilter(filter: ExpensesFilter) { _filter.value = filter }

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
        customSubCategoryId: Long? = null
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
                    customSubCategoryId = customSubCategoryId
                )
            )
                .onSuccess { _event.emit(ExpensesEvent.Saved) }
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
        customSubCategoryId: Long? = null
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
                    customSubCategoryId = customSubCategoryId
                )
            )
                .onSuccess { _event.emit(ExpensesEvent.Saved) }
                .onFailure { _event.emit(ExpensesEvent.Error(it.message ?: "Erreur")) }
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
 * Regroupe tous les critères de filtrage et de tri de la liste des transactions.
 * La méthode [apply] est pure (pas d'effet de bord) — facile à tester unitairement.
 */
data class ExpensesFilter(
    val query: String = "",
    val category: Category? = null,                         // null = toutes les catégories
    val period: FilterPeriod = FilterPeriod.CURRENT_MONTH,
    val transactionType: TransactionType? = TransactionType.EXPENSE, // null = tous les types
    val sort: SortOrder = SortOrder.DATE_DESC
) {
    // today est un paramètre pour faciliter les tests sans mocker LocalDate.now()
    fun apply(transactions: List<Transaction>, today: LocalDate = LocalDate.now()): List<Transaction> {
        val from = when (period) {
            FilterPeriod.CURRENT_MONTH -> today.withDayOfMonth(1)
            FilterPeriod.THREE_MONTHS  -> today.withDayOfMonth(1).minusMonths(2)
            FilterPeriod.SIX_MONTHS   -> today.withDayOfMonth(1).minusMonths(5)
            FilterPeriod.ALL           -> LocalDate.MIN
        }
        return transactions
            .filter { it.date >= from }
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
        val expenses: List<Transaction>,
        val customSubCategories: List<CustomSubCategory> = emptyList()
    ) : ExpensesUiState()
    data class Error(val message: String) : ExpensesUiState()
}

sealed class ExpensesEvent {
    data object Saved : ExpensesEvent()
    data object Deleted : ExpensesEvent()
    data class Error(val message: String) : ExpensesEvent()
}
