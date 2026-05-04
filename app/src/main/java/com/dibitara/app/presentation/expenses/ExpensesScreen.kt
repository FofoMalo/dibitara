package com.dibitara.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var showBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe les événements one-shot (ajout réussi ou erreur)
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ExpensesEvent.Added -> {
                    showBottomSheet = false
                    snackbarHostState.showSnackbar("Dépense ajoutée")
                }
                is ExpensesEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une dépense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ExpensesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ExpensesUiState.Error   -> Text(state.message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center))
                is ExpensesUiState.Success -> {
                    if (state.expenses.isEmpty()) {
                        EmptyExpenses(modifier = Modifier.align(Alignment.Center))
                    } else {
                        ExpensesList(expenses = state.expenses)
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        AddExpenseSheet(
            onAdd = { amount, category, currency, note ->
                viewModel.addExpense(amount, category, currency, note)
            },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
private fun ExpensesList(expenses: List<Transaction>) {
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
            ExpenseItem(expense = expense)
        }
    }
}

@Composable
private fun ExpenseItem(expense: Transaction) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(expense.category.label(), style = MaterialTheme.typography.bodyLarge)
                Text(expense.date.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.note.isNotBlank()) {
                    Text(expense.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = "- ${"%.2f".format(expense.amountCents / 100.0)} ${expense.currency.symbol}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
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
private fun AddExpenseSheet(
    onAdd: (String, Category, Currency, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ALIMENTATION) }
    var selectedCurrency by remember { mutableStateOf(Currency.EUR) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nouvelle dépense", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Sélecteur de catégorie
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
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

            // Sélecteur de devise
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text("${currency.name} (${currency.symbol})") },
                            onClick = { selectedCurrency = currency; currencyExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optionnel)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onAdd(amount, selectedCategory, selectedCurrency, note) },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter la dépense") }
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
    Category.AUTRE          -> "Autre"
}
