package com.dibitara.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import java.time.format.DateTimeFormatter

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Transaction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ExpensesEvent.Saved   -> { showAddSheet = false; editingExpense = null
                    snackbarHostState.showSnackbar("Dépense enregistrée") }
                is ExpensesEvent.Deleted -> snackbarHostState.showSnackbar("Dépense supprimée")
                is ExpensesEvent.Error   -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une dépense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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

    // Feuille d'ajout
    if (showAddSheet) {
        ExpenseSheet(
            expense = null,
            onSave = { amount, category, currency, note, isRecurring, recurrenceDay ->
                viewModel.addExpense(amount, category, currency, note,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Feuille d'édition
    editingExpense?.let { expense ->
        ExpenseSheet(
            expense = expense,
            onSave = { amount, category, currency, note, isRecurring, recurrenceDay ->
                viewModel.updateExpense(expense, amount, category, currency, note,
                    isRecurring = isRecurring, recurrenceDay = recurrenceDay)
            },
            onDismiss = { editingExpense = null }
        )
    }
}

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
        item {
            Text("Dépenses du mois", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp))
        }
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
                    Text(expense.category.label(), style = MaterialTheme.typography.bodyLarge)
                    // Badge "↺" affiché uniquement sur les modèles récurrents
                    if (expense.isRecurring) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Récurrente",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(expense.date.format(formatter), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.note.isNotBlank()) {
                    Text(expense.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "- ${"%.2f".format(expense.amountCents / 100.0)} ${expense.currency.symbol}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la dépense ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aucune dépense ce mois-ci", style = MaterialTheme.typography.bodyLarge)
        Text("Appuyez sur + pour en ajouter une", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    expense: Transaction?,
    onSave: (String, Category, Currency, String, Boolean, Int?) -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (expense == null) "Nouvelle dépense" else "Modifier la dépense",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory.label(), onValueChange = {},
                    readOnly = true, label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    Category.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label()) },
                            onClick = { selectedCategory = cat; categoryExpanded = false }
                        )
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
                label = { Text("Note (optionnel)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            // Toggle récurrence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dépense récurrente", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isRecurring, onCheckedChange = {
                    isRecurring = it
                    if (!it) recurrenceDayStr = ""
                })
            }

            // Champ jour affiché uniquement si récurrente
            if (isRecurring) {
                OutlinedTextField(
                    value = recurrenceDayStr,
                    onValueChange = { v ->
                        // N'accepte que les chiffres et limite à 28
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
                onClick = { onSave(amount, selectedCategory, selectedCurrency, note, isRecurring, recurrenceDay) },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (expense == null) "Ajouter la dépense" else "Enregistrer les modifications") }
        }
    }
}

private fun Category.label(): String = when (this) {
    Category.ALIMENTATION   -> "Alimentation"
    Category.LOGEMENT       -> "Logement"
    Category.TRANSPORT      -> "Transport"
    Category.SANTE          -> "Santé"
    Category.LOISIRS        -> "Loisirs"
    Category.INVESTISSEMENT -> "Investissement"
    Category.EPARGNE        -> "Épargne"
    Category.ENFANT         -> "Enfant"
    Category.AUTRE          -> "Autre"
}
