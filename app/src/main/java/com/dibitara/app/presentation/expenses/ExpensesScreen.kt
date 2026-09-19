package com.dibitara.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.dibitara.app.presentation.categories.*
import com.dibitara.app.domain.model.CategoryChoice
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionSuggestion
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.presentation.common.maskIban
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    onImports: () -> Unit = {},
    onReconciliation: () -> Unit = {},
    viewModel: ExpensesViewModel = hiltViewModel(),
    actions: TransactionActionsViewModel = hiltViewModel(),
    categories: CategoryCatalogViewModel = hiltViewModel()
) {
    val catalog by categories.catalog.collectAsState()
    val categoryError by categories.error.collectAsState()
    var bulkChoice by remember { mutableStateOf<CategoryChoice?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showCategories by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    val trash by actions.corbeille.collectAsState()
    val actionMessage by actions.message.collectAsState()
    val actionBusy by actions.busy.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear  by viewModel.selectedYear.collectAsState()
    LaunchedEffect(filter, selectedMonth, selectedYear) { selectedIds = emptySet() }
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Transaction?>(null) }
    var ruleProposed by remember { mutableStateOf<ExpensesEvent.RuleSuggested?>(null) }
    var matchingRows by remember { mutableStateOf<List<Transaction>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage, showTrash, showCategories) {
        if (!showTrash && !showCategories) actionMessage?.let { snackbarHostState.showSnackbar(it); actions.effacerMessage() }
    }
    val transactionToOpen by viewModel.transactionToOpen.collectAsState()

    // Sous-catégories personnalisées - disponibles dès que le state est chargé
    val customSubCategories = (uiState as? ExpensesUiState.Success)?.customSubCategories ?: emptyList()
    val focusManager = LocalFocusManager.current

    // Ouvre automatiquement le sheet d'édition si on arrive avec un id de transaction (ex. lien depuis le Dashboard)
    LaunchedEffect(transactionToOpen) {
        transactionToOpen?.let {
            editingExpense = it
            viewModel.clearTransactionToOpen()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ExpensesEvent.Saved   -> { showAddSheet = false; editingExpense = null
                    snackbarHostState.showSnackbar("Transaction enregistrée") }
                is ExpensesEvent.Deleted -> snackbarHostState.showSnackbar("Transaction déplacée dans la corbeille")
                is ExpensesEvent.Error   -> snackbarHostState.showSnackbar(event.message)
                is ExpensesEvent.RecategorizationProposee -> Unit
                is ExpensesEvent.RuleSuggested -> ruleProposed = event
                is ExpensesEvent.RecategorizationTerminee ->
                    snackbarHostState.showSnackbar("${event.count} transaction(s) recatégorisée(s)")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Text("Activité", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))

            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = onImports) { Text("Imports et capture") }
                TextButton(onClick = onReconciliation) { Text("Rapprocher") }
                TextButton(onClick = { showTrash = true }) { Text("Corbeille (${trash.size})") }
                TextButton(onClick = { selectionMode = !selectionMode; selectedIds = emptySet() }) {
                    Text(if (selectionMode) "Terminer" else "Sélectionner")
                }
            }
            if (selectionMode) {
                Button(onClick = { showCategories = true }, enabled = selectedIds.isNotEmpty() && !actionBusy,
                    modifier = Modifier.padding(horizontal = 16.dp)) { Text("Catégoriser ${selectedIds.size} dépenses") }
            }

            // Barre de recherche + bouton filtre
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { viewModel.updateFilter(filter.copy(query = it)) },
                    placeholder = { Text("Rechercher…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Badge sur l'icône quand des filtres non-défaut sont actifs
                val activeFilterCount = filter.activeFilterCount()
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filtres",
                            tint = if (activeFilterCount > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Navigation mensuelle - visible uniquement en mode "Ce mois"
            if (filter.period == FilterPeriod.CURRENT_MONTH) {
                MonthNavigationBar(
                    month       = selectedMonth,
                    year        = selectedYear,
                    onPrevious  = viewModel::previousMonth,
                    onNext      = viewModel::nextMonth
                )
            }

            // Chip rapide "À catégoriser" - visible si des dépenses AUTRE sans sous-catégorie existent
            val currentExpenses = (uiState as? ExpensesUiState.Success)?.expenses ?: emptyList()
            val autreCount = currentExpenses.count {
                // Ne compte que les dépenses sans aucune sous-catégorie (prédéfinie ou custom)
                // Les revenus sont exclus : ils sont stockés en AUTRE par design mais ne sont pas catégorisables
                it.category == Category.AUTRE && !it.categoryConfirmed
                    && it.type == TransactionType.EXPENSE
                    && it.subCategory == null
                    && it.customSubCategoryId == null
            }
            val isCategoriserSelected = filter.uncategorizedOnly
            if (autreCount > 0 || isCategoriserSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = isCategoriserSelected,
                        onClick = {
                            if (isCategoriserSelected) {
                                viewModel.updateFilter(filter.copy(uncategorizedOnly = false))
                            } else {
                                viewModel.updateFilter(
                                    filter.copy(
                                        uncategorizedOnly = true,
                                        // category est exclusif avec uncategorizedOnly (voir apply()) :
                                        // on l'efface pour ne pas hériter d'une sélection contradictoire
                                        category = null,
                                        // Forcer EXPENSE pour ne jamais afficher les revenus dans ce filtre
                                        transactionType = TransactionType.EXPENSE
                                    )
                                )
                            }
                        },
                        label = { Text("À catégoriser · $autreCount") },
                        colors = if (autreCount > 0 && !isCategoriserSelected) {
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        }
                    )
                }
            }

            // Liste des transactions
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ExpensesUiState.Loading ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is ExpensesUiState.Error ->
                        Text(state.message, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center))
                    is ExpensesUiState.Success -> {
                        if (state.expenses.isEmpty()) {
                            EmptyExpenses(modifier = Modifier.align(Alignment.Center))
                        } else {
                            ExpensesList(
                                expenses            = state.expenses,
                                selectionMode = selectionMode,
                                selectedIds = selectedIds,
                                onToggle = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
                                customSubCategories = state.customSubCategories,
                                virementsInternesIds = state.virementsInternesIds,
                                // Le regroupement par jour n'a de sens que si la liste est déjà
                                // triée par date - sinon les mêmes jours ne seraient pas contigus.
                                groupByDay          = filter.sort == SortOrder.DATE_DESC,
                                onEdit              = { editingExpense = it },
                                onDelete            = viewModel::deleteExpense
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategories) CategoryPicker(onDismiss={showCategories=false},onChoose={bulkChoice=it;showCategories=false},viewModel=categories)
    bulkChoice?.let { choice -> AlertDialog(onDismissRequest={bulkChoice=null},title={Text("Classer la sélection")},text={Column {
        Text("${selectedIds.size} dépenses seront classées dans ${catalog.label(choice)}. Les anciennes sous-catégories seront remplacées.")
        categoryError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }},confirmButton={TextButton(onClick={categories.classify(selectedIds,choice){bulkChoice=null;selectedIds=emptySet();selectionMode=false}}){Text("Appliquer")}},dismissButton={TextButton(onClick={bulkChoice=null}){Text("Annuler")}}) }
    if (showTrash) {
        ModalBottomSheet(onDismissRequest = { showTrash = false }) {
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                item { Text("Corbeille", style = MaterialTheme.typography.headlineSmall) }
                actionMessage?.let { item { Text(it) } }
                item { Text("Les opérations restent récupérables après fermeture de l’application. Elles sont exclues des totaux et des récurrences tant qu’elles sont ici.") }
                if (trash.isEmpty()) item { Text("Aucune opération dans la corbeille.", Modifier.padding(vertical = 16.dp)) }
                items(trash, key = { it.transaction.id }) { entry ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(entry.transaction.note.ifBlank { entry.transaction.category.displayName }.maskIban())
                        Text("${entry.transaction.date} · ${entry.transaction.amountCents.toCurrencyDisplay(entry.transaction.currency)}")
                        TextButton(enabled = !actionBusy, onClick = { actions.restaurer(entry.transaction.id) }) { Text("Restaurer") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    // Bottom sheet des filtres
    if (showFilterSheet) {
        FilterSheet(
            filter = filter,
            bankAccounts = bankAccounts,
            onFilterChange = { viewModel.updateFilter(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Feuille d'ajout
    if (showAddSheet) {
        ExpenseSheet(
            expense                   = null,
            defaultCurrency           = defaultCurrency,
            bankAccounts = bankAccounts,
            customSubCategories       = customSubCategories,
            suggestions               = suggestions,
            onCreateCustomSubCategory = viewModel::creerCustomSubCategory,
            onDeleteCustomSubCategory = viewModel::supprimerCustomSubCategory,
            onSave = { amount, category, currency, note, date, isRecurring, recurrenceDay, subCategory, type, customSubCategoryId, freq, endDate, bankAccountId, categoryConfirmed ->
                viewModel.addExpense(amount, category, currency, note,
                    date = date,
                    type = type,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay,
                    subCategory = subCategory,
                    customSubCategoryId = customSubCategoryId,
                    recurrenceFrequency = freq,
                    endDate = endDate, bankAccountId = bankAccountId, categoryConfirmed = categoryConfirmed)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Feuille d'édition
    editingExpense?.let { expense ->
        ExpenseSheet(
            expense                   = expense,
            defaultCurrency           = defaultCurrency,
            bankAccounts = bankAccounts,
            customSubCategories       = customSubCategories,
            suggestions               = suggestions,
            onCreateCustomSubCategory = viewModel::creerCustomSubCategory,
            onDeleteCustomSubCategory = viewModel::supprimerCustomSubCategory,
            onSave = { amount, category, currency, note, date, isRecurring, recurrenceDay, subCategory, type, customSubCategoryId, freq, endDate, bankAccountId, categoryConfirmed ->
                viewModel.updateExpense(expense, amount, category, currency, note,
                    date = date,
                    type = type,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay,
                    subCategory = subCategory,
                    customSubCategoryId = customSubCategoryId,
                    recurrenceFrequency = freq,
                    endDate = endDate, bankAccountId = bankAccountId, categoryConfirmed = categoryConfirmed)
            },
            onDismiss = { editingExpense = null }
        )
    }

    ruleProposed?.let { proposition ->
        AlertDialog(onDismissRequest={ruleProposed=null;matchingRows=null},title={Text("Simplifier les prochains classements")},text={Column {
            Text("Toujours classer ce libellé dans ${catalog.label(proposition.choice)} ?")
            Text(proposition.note)
            Text("La règle correspond au libellé exact, sans tenir compte de la casse.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={categories.matches(proposition.note,proposition.choice){matchingRows=it}}){Text("Voir les anciennes opérations correspondantes")}
            matchingRows?.let { rows ->
                Text("${rows.size} opérations à reclasser")
                rows.take(5).forEach { Text("${it.date} · ${it.amountCents.toCurrencyDisplay(it.currency)}") }
                if(rows.isNotEmpty())TextButton(onClick={categories.classify(rows.map { it.id }.toSet(),proposition.choice){matchingRows=null;ruleProposed=null}}){Text("Reclasser ces ${rows.size} opérations")}
            }
            categoryError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(onClick={categories.remember(proposition.note,proposition.choice){ruleProposed=null;matchingRows=null}}){Text("Créer la règle")}},dismissButton={TextButton(onClick={ruleProposed=null;matchingRows=null}){Text("Pas maintenant")}})
    }
}

// ─── Barre de navigation mensuelle ───────────────────────────────────────────

@Composable
private fun MonthNavigationBar(
    month: Int,
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
        .replaceFirstChar { it.uppercase() }
    // Désactiver "suivant" si on est déjà au mois courant
    val now = LocalDate.now()
    val estMoisCourant = month == now.monthValue && year == now.year

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Mois précédent",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text  = "$monthName $year",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext, enabled = !estMoisCourant) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Mois suivant",
                tint = if (estMoisCourant)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Bottom sheet des filtres ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: ExpensesFilter,
    bankAccounts: List<BankAccount> = emptyList(),
    onFilterChange: (ExpensesFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var chooseCategory by remember { mutableStateOf(false) }
    if(chooseCategory) CategoryPicker(onDismiss={chooseCategory=false},includeArchived=true,onChoose={onFilterChange(filter.copy(category=it.category,subCategory=it.subCategory,customSubCategoryId=it.customSubCategoryId,uncategorizedOnly=false));chooseCategory=false})
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Filtres", style = MaterialTheme.typography.titleLarge)

            // Période
            Text("Période", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FilterPeriod.entries) { period ->
                    FilterChip(
                        selected = filter.period == period,
                        onClick = { onFilterChange(filter.copy(period = period)) },
                        label = { Text(period.label) }
                    )
                }
            }

            // Type
            Text("Type", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter.transactionType == null,
                        onClick = { onFilterChange(filter.copy(transactionType = null)) },
                        label = { Text("Tous") }
                    )
                }
                items(TransactionType.entries) { type ->
                    FilterChip(
                        selected = filter.transactionType == type,
                        onClick = { onFilterChange(filter.copy(transactionType = type)) },
                        label = { Text(type.label()) }
                    )
                }
            }

            Text("Catégorie",style=MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick={chooseCategory=true},modifier=Modifier.fillMaxWidth()) { Text(filter.category?.displayName ?: "Toutes les catégories") }
            if(filter.category!=null) TextButton(onClick={onFilterChange(filter.copy(category=null,subCategory=null,customSubCategoryId=null))}) { Text("Effacer le filtre catégorie") }

            // Compte bancaire (uniquement si des comptes sont configurés)
            if (bankAccounts.isNotEmpty()) {
                Text("Compte", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.bankAccountId == null,
                            onClick = { onFilterChange(filter.copy(bankAccountId = null)) },
                            label = { Text("Tous") }
                        )
                    }
                    items(bankAccounts) { compte ->
                        FilterChip(
                            selected = filter.bankAccountId == compte.id,
                            onClick = { onFilterChange(filter.copy(bankAccountId = compte.id)) },
                            label = { Text(compte.label) }
                        )
                    }
                }
            }

            // Tri
            Text("Trier par", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = filter.sort == order,
                        onClick = { onFilterChange(filter.copy(sort = order)) },
                        label = { Text(order.label) }
                    )
                }
            }

            // Réinitialiser
            if (filter.activeFilterCount() > 0) {
                TextButton(
                    onClick = { onFilterChange(ExpensesFilter()) },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Réinitialiser les filtres") }
            }
        }
    }
}

// ─── Liste ────────────────────────────────────────────────────────────────────

@Composable
private fun ExpensesList(
    expenses: List<Transaction>,
    customSubCategories: List<CustomSubCategory>,
    virementsInternesIds: Set<Long> = emptySet(),
    groupByDay: Boolean,
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onToggle: (Long) -> Unit = {},
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    // Index par id pour lookup O(1) dans chaque item
    val customSubCatById = remember(customSubCategories) { customSubCategories.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // bottom = 160.dp pour ne pas laisser le FAB (Scaffold ne réserve pas d'espace pour lui)
        // chevaucher la dernière ligne de transaction - 96.dp testé insuffisant sur appareil réel
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groupByDay) {
            // La liste est triée par date desc : groupBy préserve l'ordre de première
            // rencontre des clés, donc les jours restent contigus et dans le bon ordre.
            val groupes = expenses.groupBy { it.date }
            groupes.forEach { (date, expensesDuJour) ->
                item(key = "jour_$date") {
                    DayHeader(date = date, expenses = expensesDuJour, modifier = Modifier.padding(top = 4.dp))
                }
                items(expensesDuJour, key = { it.id }) { expense ->
                    ExpenseItem(
                        expense                 = expense,
                        selectionMode = selectionMode,
                        selected = expense.id in selectedIds,
                        onToggle = { onToggle(expense.id) },
                        customSubCategoryName   = expense.customSubCategoryId?.let { customSubCatById[it]?.name },
                        virementInterne         = expense.id in virementsInternesIds,
                        onEdit                  = { onEdit(expense) },
                        onDelete                = { onDelete(expense) }
                    )
                }
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseItem(
                    expense                 = expense,
                        selectionMode = selectionMode,
                        selected = expense.id in selectedIds,
                        onToggle = { onToggle(expense.id) },
                    customSubCategoryName   = expense.customSubCategoryId?.let { customSubCatById[it]?.name },
                    virementInterne         = expense.id in virementsInternesIds,
                    onEdit                  = { onEdit(expense) },
                    onDelete                = { onDelete(expense) }
                )
            }
        }
    }
}

/** En-tête de groupe "Aujourd'hui" / "Hier" / date + total net du jour (si une seule devise). */
@Composable
private fun DayHeader(date: LocalDate, expenses: List<Transaction>, modifier: Modifier = Modifier) {
    val devises = expenses.map { it.currency }.distinct()
    val totalCents = if (devises.size == 1) {
        expenses.sumOf { if (it.type == TransactionType.EXPENSE) -it.amountCents else it.amountCents }
    } else null

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel(date),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (totalCents != null) {
            Text(
                text = (if (totalCents > 0) "+" else "") + totalCents.toCurrencyDisplay(devises.first()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalCents >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today               -> "Aujourd'hui"
        today.minusDays(1)  -> "Hier"
        else -> {
            val pattern = if (date.year == today.year) "d MMMM" else "d MMMM yyyy"
            date.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
        }
    }
}

@Composable
internal fun ExpenseItem(
    expense: Transaction,
    customSubCategoryName: String?,
    virementInterne: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggle: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val estRevenu = expense.type == TransactionType.INCOME
    val selectable = expense.type == TransactionType.EXPENSE
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !selectionMode || selectable) {
                if (selectionMode) onToggle() else onEdit()
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectionMode) Checkbox(checked = selected, onCheckedChange = null, enabled = selectable)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(expense.note.ifBlank { if (estRevenu) "Revenu" else expense.category.displayName }.maskIban(),
                    style = MaterialTheme.typography.bodyLarge)
                val category = expense.categoryPath ?: customSubCategoryName ?: expense.subCategory?.displayName ?: expense.category.displayName
                Text("${expense.date.format(DateTimeFormatter.ofPattern("dd/MM"))} · ${if (virementInterne) "Virement interne" else category}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${if (estRevenu) "+" else "−"} ${expense.amountCents.toCurrencyDisplay(expense.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (estRevenu) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                if (expense.isRecurring) Text("Récurrente", style = MaterialTheme.typography.labelSmall)
            }
            if (!selectionMode) Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "Actions") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Mettre à la corbeille") }, onClick = { showMenu = false; showDeleteConfirm = true })
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
    if (showDeleteConfirm) AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Mettre à la corbeille ?") },
        text = { Text("Vous pourrez restaurer cette opération depuis la corbeille. Une opération récurrente mise à la corbeille ne générera plus de nouvelles échéances.") },
        confirmButton = { TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Confirmer") } },
        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") } }
    )
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aucune transaction trouvée", style = MaterialTheme.typography.bodyLarge)
        Text("Modifiez les filtres ou appuyez sur +", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Feuille de saisie ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    expense: Transaction?,
    defaultCurrency: Currency = Currency.EUR,
    bankAccounts: List<BankAccount> = emptyList(),
    customSubCategories: List<CustomSubCategory>,
    suggestions: List<TransactionSuggestion> = emptyList(),
    onCreateCustomSubCategory: (String, Category) -> Unit,
    onDeleteCustomSubCategory: (CustomSubCategory) -> Unit,
    onSave: (String, Category, Currency, String, LocalDate, Boolean, Int?, SubCategory?, TransactionType, Long?, com.dibitara.app.domain.model.RecurrenceFrequency?, LocalDate?, Long?, Boolean) -> Unit,
    onDismiss: () -> Unit,
    categories: CategoryCatalogViewModel = hiltViewModel()
) {
    val catalog by categories.catalog.collectAsState()
    var showCategoryPicker by remember { mutableStateOf(false) }
    val ruleSuggestion by categories.suggestion.collectAsState()
    // Toujours formater avec un point - "%.2f" utilise la locale système (virgule sur FR)
    var amount by remember { mutableStateOf(expense?.let { "%.2f".format(it.amountCents / 100.0).replace(',', '.') } ?: "") }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    LaunchedEffect(note) { categories.suggest(note) }
    var selectedAccountId by remember { mutableStateOf(expense?.bankAccountId) }
    var accountExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(expense?.category ?: Category.ALIMENTATION) }
    var categoryConfirmed by remember { mutableStateOf(expense?.categoryConfirmed ?: true) }
    var selectedCurrency by remember { mutableStateOf(expense?.currency ?: defaultCurrency) }
    var selectedDate by remember { mutableStateOf(expense?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(expense?.isRecurring ?: false) }
    var recurrenceDayStr by remember { mutableStateOf(expense?.recurrenceDay?.toString() ?: "") }
    var selectedFrequency by remember {
        mutableStateOf(expense?.recurrenceFrequency ?: com.dibitara.app.domain.model.RecurrenceFrequency.MONTHLY)
    }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf(expense?.endDate) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var selectedSubCategory by remember { mutableStateOf(expense?.subCategory) }
    // Sous-catégorie personnalisée sélectionnée (cherchée par id à l'ouverture)
    var selectedCustomSubCategory by remember {
        mutableStateOf(expense?.customSubCategoryId?.let { id -> customSubCategories.find { it.id == id } })
    }
    // Dépense par défaut ; on relit le type si on édite une transaction existante
    var selectedType by remember { mutableStateOf(expense?.type ?: TransactionType.EXPENSE) }
    val focusManager = LocalFocusManager.current
    // FocusRequester pour sauter les champs readOnly (date, catégorie, devise) lors de la navigation IME
    val noteFocusRequester = remember { FocusRequester() }
    val dayFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (selectedType == TransactionType.INCOME) {
                    if (expense == null) "Nouveau revenu" else "Modifier le revenu"
                } else {
                    if (expense == null) "Nouvelle dépense" else "Modifier la dépense"
                },
                style = MaterialTheme.typography.titleLarge
            )

            // Sélecteur de type : Dépense ou Revenu
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Dépense") }
                )
                SegmentedButton(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedSubCategory = null
                        selectedCustomSubCategory = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Revenu") }
                )
            }

            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { noteFocusRequester.requestFocus() }),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Choisir une date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedType == TransactionType.EXPENSE) {
                OutlinedButton(onClick={showCategoryPicker=true},modifier=Modifier.fillMaxWidth()) {
                    Text(catalog.label(CategoryChoice(selectedCategory,selectedSubCategory,selectedCustomSubCategory?.id)))
                }
            }

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                OutlinedTextField(
                    value = bankAccounts.firstOrNull { it.id == selectedAccountId }?.label ?: "Non rattachée",
                    onValueChange = {}, readOnly = true, label = { Text("Compte bancaire") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                    DropdownMenuItem(text = { Text("Non rattachée") }, onClick = { selectedAccountId = null; accountExpanded = false })
                    bankAccounts.forEach { account ->
                        DropdownMenuItem(text = { Text(account.label) }, onClick = { selectedAccountId = account.id; accountExpanded = false })
                    }
                }
            }
            if (expense != null) {
                Text(
                    when {
                        expense.importSource?.contains("notification") == true -> "Origine : capture de notification"
                        expense.importSource != null -> "Origine : import bancaire (${expense.importSource})"
                        else -> "Origine : saisie manuelle"
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text(if (selectedType == TransactionType.INCOME) "Libellé (ex: Salaire)" else "Note (optionnel)") },
                keyboardOptions = KeyboardOptions(imeAction = if (isRecurring) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onNext = { dayFocusRequester.requestFocus() },
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(noteFocusRequester)
            )

            // Chips de suggestion - filtrées sur ce que l'utilisateur a tapé dans la note
            val suggestionsFiltrées = remember(note, suggestions) {
                if (note.isBlank()) emptyList()
                else suggestions.filter { it.label.contains(note.trim(), ignoreCase = true) }
            }
            if (suggestionsFiltrées.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestionsFiltrées) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                // Pré-remplit tous les champs en un seul tap
                                note = suggestion.label
                                amount = "%.2f".format(suggestion.amountCents / 100.0).replace(',', '.')
                                categoryConfirmed=true
                                selectedCategory = suggestion.category
                                selectedCurrency = suggestion.currency
                                selectedType = suggestion.type
                                selectedSubCategory = suggestion.subCategory
                                selectedCustomSubCategory = suggestion.customSubCategoryId
                                    ?.let { id -> customSubCategories.find { it.id == id } }
                            },
                            label = {
                                Text(
                                    "${suggestion.label}  " +
                                    "${"%.2f".format(suggestion.amountCents / 100.0)} " +
                                    suggestion.currency.symbol
                                )
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Récurrente", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isRecurring, onCheckedChange = {
                    isRecurring = it
                    if (!it) {
                        recurrenceDayStr = ""
                        endDate = null
                    } else if (recurrenceDayStr.isEmpty()) {
                        recurrenceDayStr = selectedDate.dayOfMonth.coerceAtMost(28).toString()
                    }
                })
            }

            if (isRecurring) {
                // Sélecteur de fréquence : Mensuelle / Hebdomadaire / Annuelle
                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFrequency.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fréquence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false }
                    ) {
                        com.dibitara.app.domain.model.RecurrenceFrequency.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.displayName) },
                                onClick = {
                                    selectedFrequency = freq
                                    frequencyExpanded = false
                                    // Réinitialise le champ jour si on quitte MONTHLY
                                    if (freq != com.dibitara.app.domain.model.RecurrenceFrequency.MONTHLY) {
                                        recurrenceDayStr = ""
                                    } else if (recurrenceDayStr.isEmpty()) {
                                        recurrenceDayStr = selectedDate.dayOfMonth.coerceAtMost(28).toString()
                                    }
                                }
                            )
                        }
                    }
                }

                // Champ "Jour du mois" uniquement pour MONTHLY
                if (selectedFrequency == com.dibitara.app.domain.model.RecurrenceFrequency.MONTHLY) {
                    OutlinedTextField(
                        value = recurrenceDayStr,
                        onValueChange = { v ->
                            val n = v.filter { it.isDigit() }.take(2)
                            recurrenceDayStr = if (n.toIntOrNull()?.let { it > 28 } == true) "28" else n
                        },
                        label = { Text("Jour du mois (1-28)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(dayFocusRequester)
                    )
                }

                // Date de fin optionnelle
                OutlinedTextField(
                    value = endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Indéfiniment",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de fin (optionnel)") },
                    trailingIcon = {
                        Row {
                            if (endDate != null) {
                                IconButton(onClick = { endDate = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Supprimer la date de fin")
                                }
                            }
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Choisir une date de fin")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val recurrenceDay = recurrenceDayStr.toIntOrNull()
            val monthlyValid = selectedFrequency != com.dibitara.app.domain.model.RecurrenceFrequency.MONTHLY || recurrenceDay != null
            val currentChoice = CategoryChoice(selectedCategory,selectedSubCategory,selectedCustomSubCategory?.id)
            val originalChoice = expense?.let { CategoryChoice(it.category,it.subCategory,it.customSubCategoryId) }
            val categoryAvailable = selectedType!=TransactionType.EXPENSE || currentChoice==originalChoice || catalog.node(currentChoice.key)?.let { catalog.selectable(it) }==true
            if(!categoryAvailable) Text("Choisissez une catégorie active.",color=MaterialTheme.colorScheme.error)
            val saveEnabled = categoryAvailable && amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true
                    && (!isRecurring || monthlyValid)

            Button(
                onClick = {
                    // Pour un revenu, la catégorie n'a pas de sens sémantique - on stocke AUTRE en base
                    val catFinale = if (selectedType == TransactionType.INCOME) Category.AUTRE else selectedCategory
                    // Les anciennes sous-catégories peuvent maintenant appartenir à tous les parents.
                    val subCatFinale = selectedSubCategory.takeIf {
                        selectedType == TransactionType.EXPENSE
                                && selectedCustomSubCategory == null
                    }
                    val customSubCatIdFinale = selectedCustomSubCategory?.id.takeIf {
                        selectedType == TransactionType.EXPENSE
                    }
                    val freqFinale = if (isRecurring) selectedFrequency else null
                    onSave(amount, catFinale, selectedCurrency, note, selectedDate,
                        isRecurring, recurrenceDay, subCatFinale, selectedType, customSubCatIdFinale,
                        freqFinale, endDate, selectedAccountId, categoryConfirmed)
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (expense == null) "Ajouter" else "Enregistrer les modifications") }
        }
    }

    // Sélecteur de date (ouvert via le champ Date)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            // Material3 DatePicker travaille en millisecondes UTC depuis l'epoch
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Sélecteur de date de fin de récurrence
    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (endDate ?: LocalDate.now().plusMonths(1)).toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        endDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if(showCategoryPicker) CategoryPicker(onDismiss={showCategoryPicker=false},onChoose={choice ->
        categoryConfirmed=true;selectedCategory=choice.category;selectedSubCategory=choice.subCategory
        selectedCustomSubCategory=choice.customSubCategoryId?.let { id ->
            CustomSubCategory(id,catalog.node("u:$id")?.name ?: "Sous-catégorie",choice.category)
        };showCategoryPicker=false
    },suggestion=ruleSuggestion,viewModel=categories)
}

private fun TransactionType.label(): String = when (this) {
    TransactionType.EXPENSE    -> "Dépenses"
    TransactionType.INCOME     -> "Revenus"
    TransactionType.INVESTMENT -> "Investissements"
}

// Compte le nombre de filtres actifs non-défaut pour le badge
private fun ExpensesFilter.activeFilterCount(): Int {
    var count = 0
    if (period != FilterPeriod.CURRENT_MONTH) count++
    if (transactionType != TransactionType.EXPENSE) count++
    if (category != null) count++
    if (sort != SortOrder.DATE_DESC) count++
    if (bankAccountId != null) count++
    if (uncategorizedOnly) count++
    return count
}
