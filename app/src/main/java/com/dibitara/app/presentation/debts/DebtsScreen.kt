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
import androidx.compose.material.icons.filled.Calculate
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
import com.dibitara.app.domain.model.SimulateurCredit
import com.dibitara.app.presentation.common.toCurrencyDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val confirmedDebtIds by viewModel.confirmedDebtIds.collectAsState()
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
                is DebtsEvent.VersementConfirme -> {
                    val montant = event.montantCents.toCurrencyDisplay(event.currency)
                    snackbarHostState.showSnackbar("Versement confirmé - capital réduit de $montant")
                }
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
                        debts                  = state.debts,
                        totalCents             = state.totalCents,
                        totalMonthlyCents      = state.totalMonthlyCents,
                        summaryCurrency        = state.summaryCurrency,
                        confirmedDebtIds       = confirmedDebtIds,
                        onDelete               = viewModel::removeDebt,
                        onEdit                 = { debt -> debtToEdit = debt },
                        onConfirmerVersement   = viewModel::confirmerVersement
                    )
            }
        }
    }

    if (showAddSheet) {
        AddDebtSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, total, monthly, original, paymentDay, taux, currency, type ->
                viewModel.addDebt(label, total, monthly, original, paymentDay, taux, currency, type)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    debtToEdit?.let { debt ->
        EditDebtSheet(
            debt = debt,
            onSave = { label, total, monthly, original, paymentDay, taux, currency, type ->
                viewModel.editDebt(debt, label, total, monthly, original, paymentDay, taux, currency, type)
            },
            onDismiss = { debtToEdit = null }
        )
    }
}

@Composable
private fun DebtsContent(
    debts                : List<Debt>,
    totalCents           : Long,
    totalMonthlyCents    : Long,
    summaryCurrency      : Currency,
    confirmedDebtIds     : Set<Long> = emptySet(),
    onDelete             : (Debt) -> Unit,
    onEdit               : (Debt) -> Unit = {},
    onConfirmerVersement : (Debt) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Carte récapitulative - tertiaryContainer, pas errorContainer (une dette n'est pas une urgence)
        if (debts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Total restant dû",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                totalCents.toCurrencyDisplay(summaryCurrency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Mensualités",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${totalMonthlyCents.toCurrencyDisplay(summaryCurrency)}/mois",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
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
                DebtCard(
                    debt                 = debt,
                    isConfirmedThisSession = debt.id in confirmedDebtIds,
                    onDelete             = { onDelete(debt) },
                    onEdit               = { onEdit(debt) },
                    onConfirmerVersement = { onConfirmerVersement(debt) }
                )
            }
        }
    }
}

@Composable
private fun DebtCard(
    debt                   : Debt,
    isConfirmedThisSession : Boolean = false,
    onDelete               : () -> Unit,
    onEdit                 : () -> Unit = {},
    onConfirmerVersement   : () -> Unit = {}
) {
    var showConfirm    by remember { mutableStateOf(false) }
    var showSimulation by remember { mutableStateOf(false) }

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
                    // Couleur neutre : le capital restant n'est pas une alerte permanente
                    Text(
                        "Restant dû : ${debt.totalCents.toCurrencyDisplay(debt.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (debt.monthlyPaymentCents > 0) {
                        Text(
                            "${debt.monthlyPaymentCents.toCurrencyDisplay(debt.currency)}/mois" +
                                (debt.tauxInteret?.let { " · ${String.format("%.2f", it).replace('.', ',')} %" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    // Bouton simulation - affiché uniquement si le taux est renseigné
                    if (debt.tauxInteret != null && debt.monthlyPaymentCents > 0) {
                        IconButton(onClick = { showSimulation = true }) {
                            Icon(Icons.Filled.Calculate, contentDescription = "Simuler remboursement anticipé",
                                tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Hint si capital d'origine non renseigné
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

            // Échéance estimée - affichée en date lisible plutôt qu'en mois bruts
            if (debt.monthlyPaymentCents > 0 && debt.totalCents > 0) {
                val nbMois = (debt.totalCents / debt.monthlyPaymentCents).toInt()
                val texteEcheance = when {
                    nbMois <= 0  -> "presque remboursé"
                    nbMois < 12  -> "dans $nbMois mois"
                    else -> {
                        val echeance = java.time.LocalDate.now().plusMonths(nbMois.toLong())
                        val nomMois  = echeance.month
                            .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH)
                            .replaceFirstChar { it.uppercase() }
                        "Fin $nomMois ${echeance.year}"
                    }
                }
                Text(
                    "Échéance estimée : $texteEcheance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Hint simulation si taux non renseigné
            if (debt.tauxInteret == null && debt.type == DebtType.CREDIT_IMMO) {
                Text(
                    "Ajoutez le taux d'intérêt (✎) pour simuler un remboursement anticipé",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Bouton de confirmation du prélèvement mensuel
            // Affiché si le jour de prélèvement est renseigné et qu'on est dans la fenêtre du mois
            // (3 jours avant → fin du mois), et que l'utilisateur n'a pas déjà confirmé cette session
            val payDay = debt.paymentDay
            if (payDay != null && debt.monthlyPaymentCents > 0 && !isConfirmedThisSession) {
                val aujourd_hui = java.time.LocalDate.now()
                val jourActuel  = aujourd_hui.dayOfMonth
                if (jourActuel >= (payDay - 3).coerceAtLeast(1)) {
                    val nomMois = aujourd_hui.month
                        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() }
                    OutlinedButton(
                        onClick = onConfirmerVersement,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Valider le prélèvement du $payDay $nomMois")
                    }
                }
            }
        }
    }

    if (showSimulation && debt.tauxInteret != null) {
        SimulationSheet(debt = debt, onDismiss = { showSimulation = false })
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
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            type.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, total: String, monthly: String, original: String, paymentDay: Int?, taux: String, currency: Currency, type: DebtType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var original by remember { mutableStateOf("") }
    var paymentDayStr by remember { mutableStateOf("") }
    var tauxStr by remember { mutableStateOf("") }
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

            OutlinedTextField(
                value = tauxStr,
                onValueChange = { tauxStr = it },
                label = { Text("Taux d'intérêt annuel en % (ex : 1,85)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
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
                onClick = { onSave(label, total, monthly, original, paymentDayInt, tauxStr, selectedCurrency, selectedType) },
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
    onSave: (label: String, total: String, monthly: String, original: String, paymentDay: Int?, taux: String, currency: Currency, type: DebtType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(debt.label) }
    var total by remember { mutableStateOf((debt.totalCents.toDouble() / 100.0).toString()) }
    var monthly by remember { mutableStateOf(if (debt.monthlyPaymentCents > 0) (debt.monthlyPaymentCents.toDouble() / 100.0).toString() else "") }
    var original by remember { mutableStateOf(if (debt.originalAmountCents > 0) (debt.originalAmountCents.toDouble() / 100.0).toString() else "") }
    var paymentDayStr by remember { mutableStateOf(debt.paymentDay?.toString() ?: "") }
    var tauxStr by remember { mutableStateOf(debt.tauxInteret?.let { String.format("%.2f", it).replace('.', ',') } ?: "") }
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                isError = paymentDayError,
                supportingText = if (paymentDayError) { { Text("Entrez un jour entre 1 et 28") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tauxStr,
                onValueChange = { tauxStr = it },
                label = { Text("Taux d'intérêt annuel en % (ex : 1,85)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
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
                onClick = { onSave(label, total, monthly, original, paymentDayInt, tauxStr, selectedCurrency, selectedType) },
                enabled = label.isNotBlank() && total.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true && !paymentDayError,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }
        }
    }
}

// ─── Simulation remboursement anticipé ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulationSheet(debt: Debt, onDismiss: () -> Unit) {
    val taux = debt.tauxInteret ?: return
    var montantStr by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val montantCents = montantStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
    val resultat = if (montantCents > 0) {
        SimulateurCredit.simuler(
            capitalRestantCents        = debt.totalCents,
            tauxAnnuelPct              = taux,
            mensualiteCents            = debt.monthlyPaymentCents,
            remboursementAnticipeCents = montantCents
        )
    } else null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Simulation - Remboursement anticipé", style = MaterialTheme.typography.titleLarge)
            Text(
                "${debt.label} · ${String.format("%.2f", taux).replace('.', ',')} % · " +
                    "${debt.totalCents.toCurrencyDisplay(debt.currency)} restant",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = montantStr,
                onValueChange = { montantStr = it },
                label = { Text("Montant du remboursement anticipé (${debt.currency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (resultat != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SimLigne("Durée actuelle",
                            "${resultat.moisActuels} mois (${resultat.moisActuels / 12} ans ${resultat.moisActuels % 12} mois)")
                        SimLigne("Durée après RA",
                            "${resultat.moisApres} mois (${resultat.moisApres / 12} ans ${resultat.moisApres % 12} mois)")
                        HorizontalDivider()
                        SimLigneValeur("Mois économisés",
                            "${resultat.moisEconomises} mois",
                            MaterialTheme.colorScheme.primary)
                        SimLigneValeur("Intérêts économisés",
                            resultat.interetsEconomisesCents.toCurrencyDisplay(debt.currency),
                            MaterialTheme.colorScheme.primary)
                        resultat.nouvelleEcheanceAnneeMois?.let { (annee, mois) ->
                            val nomMois = java.time.Month.of(mois)
                                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH)
                                .replaceFirstChar { it.uppercase() }
                            SimLigneValeur("Nouvelle échéance", "$nomMois $annee",
                                MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Intérêts totaux restants", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sans RA", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(resultat.interetsTotauxActuelsCents.toCurrencyDisplay(debt.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Text("→", modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avec RA", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(resultat.interetsTotauxApresCents.toCurrencyDisplay(debt.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else if (montantStr.isNotBlank()) {
                Text("Données insuffisantes (vérifiez le taux et la mensualité).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SimLigne(label: String, valeur: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(valeur, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SimLigneValeur(label: String, valeur: String, color: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Text(valeur, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = color)
    }
}
