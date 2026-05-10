package com.dibitara.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.format.DateTimeFormatter

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Transaction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ExpensesEvent.Saved   -> { showAddSheet = false; editingExpense = null
                    snackbarHostState.showSnackbar("Transaction enregistrée") }
                is ExpensesEvent.Deleted -> snackbarHostState.showSnackbar("Transaction supprimée")
                is ExpensesEvent.Error   -> snackbarHostState.showSnackbar(event.message)
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
                                expenses = state.expenses,
                                onEdit = { editingExpense = it },
                                onDelete = viewModel::deleteExpense
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet des filtres
    if (showFilterSheet) {
        FilterSheet(
            filter = filter,
            onFilterChange = { viewModel.updateFilter(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Feuille d'ajout
    if (showAddSheet) {
        ExpenseSheet(
            expense = null,
            onSave = { amount, category, currency, note, isRecurring, recurrenceDay, subCategory, type ->
                viewModel.addExpense(amount, category, currency, note,
                    type = type,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay,
                    subCategory = subCategory)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Feuille d'édition
    editingExpense?.let { expense ->
        ExpenseSheet(
            expense = expense,
            onSave = { amount, category, currency, note, isRecurring, recurrenceDay, subCategory, type ->
                viewModel.updateExpense(expense, amount, category, currency, note,
                    type = type,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay,
                    subCategory = subCategory)
            },
            onDismiss = { editingExpense = null }
        )
    }
}

// ─── Bottom sheet des filtres ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: ExpensesFilter,
    onFilterChange: (ExpensesFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            // Catégorie
            Text("Catégorie", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter.category == null,
                        onClick = { onFilterChange(filter.copy(category = null)) },
                        label = { Text("Toutes") }
                    )
                }
                items(Category.entries) { cat ->
                    FilterChip(
                        selected = filter.category == cat,
                        onClick = { onFilterChange(filter.copy(category = cat)) },
                        label = { Text(cat.displayName) }
                    )
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
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses, key = { it.id }) { expense ->
            ExpenseItem(expense = expense, onEdit = { onEdit(expense) }, onDelete = { onDelete(expense) })
        }
    }
}

@Composable
private fun ExpenseItem(expense: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pour un revenu, la note est plus significative que la catégorie (stockée AUTRE)
                    val labelPrincipal = if (expense.type == TransactionType.INCOME)
                        expense.note.ifBlank { "Revenu" }
                    else
                        expense.category.displayName
                    Text(labelPrincipal, style = MaterialTheme.typography.bodyLarge)
                    if (expense.isRecurring) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Récurrente",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (expense.type == TransactionType.EXPENSE && expense.subCategory != null) {
                    Text(expense.subCategory.displayName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(expense.date.format(formatter), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Pour les revenus, la note est déjà utilisée comme label principal
                if (expense.type == TransactionType.EXPENSE && expense.note.isNotBlank()) {
                    Text(expense.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${if (expense.type == TransactionType.EXPENSE) "-" else "+"} ${"%.2f".format(expense.amountCents / 100.0)} ${expense.currency.symbol}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (expense.type == TransactionType.EXPENSE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la transaction ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            }
        )
    }
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
    onSave: (String, Category, Currency, String, Boolean, Int?, SubCategory?, TransactionType) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(expense?.let { "%.2f".format(it.amountCents / 100.0) } ?: "") }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    var selectedCategory by remember { mutableStateOf(expense?.category ?: Category.ALIMENTATION) }
    var selectedCurrency by remember { mutableStateOf(expense?.currency ?: Currency.EUR) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(expense?.isRecurring ?: false) }
    var recurrenceDayStr by remember { mutableStateOf(expense?.recurrenceDay?.toString() ?: "") }
    var selectedSubCategory by remember { mutableStateOf(expense?.subCategory) }
    var subCategoryExpanded by remember { mutableStateOf(false) }
    // Dépense par défaut ; on relit le type si on édite une transaction existante
    var selectedType by remember { mutableStateOf(expense?.type ?: TransactionType.EXPENSE) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
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
                    onClick = { selectedType = TransactionType.INCOME; selectedSubCategory = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Revenu") }
                )
            }

            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            // Catégorie + sous-catégorie — sans sens pour un revenu, masquées
            if (selectedType == TransactionType.EXPENSE) {
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = selectedCategory.displayName, onValueChange = {},
                        readOnly = true, label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        Category.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    // Réinitialise la sous-catégorie si on quitte AUTRE
                                    if (cat != Category.AUTRE) selectedSubCategory = null
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sous-catégorie — visible uniquement pour AUTRE
                if (selectedCategory == Category.AUTRE) {
                    ExposedDropdownMenuBox(expanded = subCategoryExpanded, onExpandedChange = { subCategoryExpanded = it }) {
                        OutlinedTextField(
                            value = selectedSubCategory?.displayName ?: "Sélectionner…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sous-catégorie (optionnel)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subCategoryExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = subCategoryExpanded, onDismissRequest = { subCategoryExpanded = false }) {
                            SubCategory.entries.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.displayName) },
                                    onClick = { selectedSubCategory = sub; subCategoryExpanded = false }
                                )
                            }
                        }
                    }
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

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text(if (selectedType == TransactionType.INCOME) "Libellé (ex: Salaire Florent)" else "Note (optionnel)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Récurrente", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isRecurring, onCheckedChange = {
                    isRecurring = it
                    if (!it) recurrenceDayStr = ""
                })
            }

            if (isRecurring) {
                OutlinedTextField(
                    value = recurrenceDayStr,
                    onValueChange = { v ->
                        val n = v.filter { it.isDigit() }.take(2)
                        recurrenceDayStr = if (n.toIntOrNull()?.let { it > 28 } == true) "28" else n
                    },
                    label = { Text("Jour du mois (1-28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val recurrenceDay = recurrenceDayStr.toIntOrNull()
            val saveEnabled = amount.toDoubleOrNull()?.let { it > 0 } == true
                    && (!isRecurring || recurrenceDay != null)

            Button(
                onClick = {
                    // Pour un revenu, la catégorie n'a pas de sens sémantique — on stocke AUTRE en base
                    val catFinale = if (selectedType == TransactionType.INCOME) Category.AUTRE else selectedCategory
                    val subCatFinale = selectedSubCategory.takeIf {
                        selectedType == TransactionType.EXPENSE && selectedCategory == Category.AUTRE
                    }
                    onSave(amount, catFinale, selectedCurrency, note, isRecurring, recurrenceDay,
                        subCatFinale, selectedType)
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (expense == null) "Ajouter" else "Enregistrer les modifications") }
        }
    }
}

// ─── Extensions d'affichage ───────────────────────────────────────────────────

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
    return count
}
