package com.dibitara.app.presentation.debts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.presentation.common.toCurrencyDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<Debt?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is DebtsEvent.Saved   -> {
                    showAddSheet = false
                    debtToEdit = null
                    snackbarHostState.showSnackbar("Dette enregistrée")
                }
                is DebtsEvent.Deleted -> snackbarHostState.showSnackbar("Dette supprimée")
                is DebtsEvent.Error   -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettes & crédits") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une dette")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is DebtsUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is DebtsUiState.Error ->
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                is DebtsUiState.Success ->
                    DebtsContent(
                        debts             = state.debts,
                        totalCents        = state.totalCents,
                        totalMonthlyCents = state.totalMonthlyCents,
                        summaryCurrency   = state.summaryCurrency,
                        onDelete          = viewModel::removeDebt,
                        onEdit            = { debt -> debtToEdit = debt }
                    )
            }
        }
    }

    if (showAddSheet) {
        AddDebtSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, total, monthly, original, paymentDay, currency, type ->
                viewModel.addDebt(label, total, monthly, original, paymentDay, currency, type)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    debtToEdit?.let { debt ->
        EditDebtSheet(
            debt = debt,
            onSave = { label, total, monthly, original, paymentDay, currency, type ->
                viewModel.editDebt(debt, label, total, monthly, original, paymentDay, currency, type)
            },
            onDismiss = { debtToEdit = null }
        )
    }
}

@Composable
private fun DebtsContent(
    debts             : List<Debt>,
    totalCents        : Long,
    totalMonthlyCents : Long,
    summaryCurrency   : Currency,
    onDelete          : (Debt) -> Unit,
    onEdit            : (Debt) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Carte récapitulative
        if (debts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Total restant dû",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                totalCents.toCurrencyDisplay(summaryCurrency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Mensualités",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${totalMonthlyCents.toCurrencyDisplay(summaryCurrency)}/mois",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune dette enregistrée.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                DebtCard(debt = debt, onDelete = { onDelete(debt) }, onEdit = { onEdit(debt) })
            }
        }
    }
}

@Composable
private fun DebtCard(debt: Debt, onDelete: () -> Unit, onEdit: () -> Unit = {}) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebtTypeChip(type = debt.type)
                        Text(debt.label, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        "Restant dû : ${debt.totalCents.toCurrencyDisplay(debt.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (debt.monthlyPaymentCents > 0) {
                        Text(
                            "${debt.monthlyPaymentCents.toCurrencyDisplay(debt.currency)}/mois",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Hint si capital d'origine non renseigné (dettes migrées ou création sans ce champ)
            if (debt.originalAmountCents == 0L) {
                Text(
                    "Ajoutez le capital d'origine (✎) pour voir la progression",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Barre de progression si capital d'origine renseigné
            if (debt.originalAmountCents > 0 && debt.totalCents <= debt.originalAmountCents) {
                val progress = 1f - (debt.totalCents.toFloat() / debt.originalAmountCents.toFloat())
                val percentRembourse = (progress * 100).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$percentRembourse% remboursé",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Échéance estimée
            if (debt.monthlyPaymentCents > 0 && debt.totalCents > 0) {
                val nbMois = (debt.totalCents / debt.monthlyPaymentCents).toInt()
                Text(
                    "Échéance estimée : dans $nbMois mois",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer cette dette ?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun DebtTypeChip(type: DebtType) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            type.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, total: String, monthly: String, original: String, paymentDay: Int?, currency: Currency, type: DebtType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var original by remember { mutableStateOf("") }
    var paymentDayStr by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var selectedType by remember { mutableStateOf(DebtType.CREDIT_IMMO) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val paymentDayInt = paymentDayStr.toIntOrNull()?.takeIf { it in 1..28 }
    val paymentDayError = paymentDayStr.isNotEmpty() && paymentDayInt == null

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
            Text("Nouvelle dette", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    DebtType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = { selectedType = type; typeExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé (ex. Crédit immobilier résidence)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("Capital restant dû") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = monthly,
                onValueChange = { monthly = it },
                label = { Text("Mensualité (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = original,
                onValueChange = { original = it },
                label = { Text("Capital d'origine (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paymentDayStr,
                onValueChange = { paymentDayStr = it },
                label = { Text("Jour de prélèvement (1-28, optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                isError = paymentDayError,
                supportingText = if (paymentDayError) { { Text("Entrez un jour entre 1 et 28") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

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
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(label, total, monthly, original, paymentDayInt, selectedCurrency, selectedType) },
                enabled = label.isNotBlank() && total.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true && !paymentDayError,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDebtSheet(
    debt: Debt,
    onSave: (label: String, total: String, monthly: String, original: String, paymentDay: Int?, currency: Currency, type: DebtType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(debt.label) }
    var total by remember { mutableStateOf((debt.totalCents.toDouble() / 100.0).toString()) }
    var monthly by remember { mutableStateOf(if (debt.monthlyPaymentCents > 0) (debt.monthlyPaymentCents.toDouble() / 100.0).toString() else "") }
    var original by remember { mutableStateOf(if (debt.originalAmountCents > 0) (debt.originalAmountCents.toDouble() / 100.0).toString() else "") }
    var paymentDayStr by remember { mutableStateOf(debt.paymentDay?.toString() ?: "") }
    var selectedCurrency by remember { mutableStateOf(debt.currency) }
    var selectedType by remember { mutableStateOf(debt.type) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val paymentDayInt = paymentDayStr.toIntOrNull()?.takeIf { it in 1..28 }
    val paymentDayError = paymentDayStr.isNotEmpty() && paymentDayInt == null

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
            Text("Modifier la dette", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    DebtType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = { selectedType = type; typeExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("Capital restant dû") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = monthly,
                onValueChange = { monthly = it },
                label = { Text("Mensualité (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = original,
                onValueChange = { original = it },
                label = { Text("Capital d'origine (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paymentDayStr,
                onValueChange = { paymentDayStr = it },
                label = { Text("Jour de prélèvement (1-28, optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                isError = paymentDayError,
                supportingText = if (paymentDayError) { { Text("Entrez un jour entre 1 et 28") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

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
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(label, total, monthly, original, paymentDayInt, selectedCurrency, selectedType) },
                enabled = label.isNotBlank() && total.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true && !paymentDayError,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }
        }
    }
}

