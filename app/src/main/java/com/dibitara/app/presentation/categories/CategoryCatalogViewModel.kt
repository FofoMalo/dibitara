package com.dibitara.app.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryChangePreview(val source: String,val target: String,val move: Boolean,val impact: CategoryImpact)
@HiltViewModel
class CategoryCatalogViewModel @Inject constructor(
    observe: ObserveCategoryCatalogUseCase, all: GetAllTransactionsUseCase,
    private val saveNode: SaveCategoryNodeUseCase, private val createNode: CreateCategoryNodeUseCase,
    private val getImpact: CategoryImpactUseCase, private val deleteNode: DeleteUnusedCategoryUseCase,
    private val mergeNodes: MergeCategoryUseCase, private val moveNode: MoveSubCategoryUseCase,
    private val classify: ClassifyTransactionsUseCase, private val remember: RememberCategoryRuleUseCase,
    private val findMatches: FindCategoryMatchesUseCase, private val getRule: GetRuleForNoteUseCase
) : ViewModel() {
    val catalog = observe().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),CategoryCatalog())
    val recent = all().map { rows -> rows.filter { it.type==TransactionType.EXPENSE }.sortedWith(compareByDescending<Transaction>{it.date}.thenByDescending{it.id})
        .map { CategoryChoice(it.category,it.subCategory,it.customSubCategoryId).key }.distinct().take(5) }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val suggestion = MutableStateFlow<CategoryChoice?>(null)
    private var suggestionJob: kotlinx.coroutines.Job? = null
    fun suggest(note: String) {
        suggestionJob?.cancel();suggestion.value=null
        if(note.isBlank())return
        suggestionJob=viewModelScope.launch { kotlinx.coroutines.delay(200); getRule(note)?.let { suggestion.value=CategoryChoice(it.category,it.subCategory,it.customSubCategoryId) } }
    }
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val preview = MutableStateFlow<CategoryChangePreview?>(null)
    private fun action(block: suspend () -> Unit) { if(busy.value)return; busy.value=true; error.value=null; viewModelScope.launch {
        try { block() } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException)throw e; error.value=e.message ?: "Action impossible" } finally { busy.value=false }
    } }
    fun save(node: CategoryNode, done: () -> Unit = {}) = action { saveNode(node);done() }
    fun create(name: String,parent: String?,done: () -> Unit) = action { createNode(name,parent);done() }
    fun delete(key: String,done: () -> Unit) = action { deleteNode(key);done() }
    fun preview(source: String,target: String,move: Boolean) = action { preview.value=CategoryChangePreview(source,target,move,getImpact(source)) }
    fun confirm(done: () -> Unit) = action { val p=requireNotNull(preview.value); if(p.move) moveNode(p.source,p.target,p.impact) else mergeNodes(p.source,p.target,p.impact);preview.value=null;done() }
    fun classify(ids: Set<Long>,choice: CategoryChoice,done: () -> Unit) = action { classify(ids,choice);done() }
    fun remember(note: String,choice: CategoryChoice,done: () -> Unit) = action { remember(note,choice);done() }
    fun matches(note: String,choice: CategoryChoice,done: (List<Transaction>) -> Unit) = action { done(findMatches(note,choice)) }
}
