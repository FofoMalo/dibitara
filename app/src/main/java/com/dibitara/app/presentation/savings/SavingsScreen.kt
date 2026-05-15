package com.dibitara.app.presentation.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.presentation.common.toCurrencyDisplay

@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddChild by remember { mutableStateOf(false) }
    // Compte à modifier : null = pas d'édition en cours
    var accountToEdit by remember { mutableStateOf<SavingsAccount?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is SavingsEvent.Saved             -> {
                    showAddSheet = false
                    accountToEdit = null
                    snackbarHostState.showSnackbar("Compte enregistré")
                }
                is SavingsEvent.Deleted           -> snackbarHostState.showSnackbar("Compte supprimé")
                is SavingsEvent.ChildSaved        -> { showAddChild = false; snackbarHostState.showSnackbar("Enfant ajouté") }
                is SavingsEvent.VersementApplique -> snackbarHostState.showSnackbar("Versement appliqué ✓")
                is SavingsEvent.Error             -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un compte épargne")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SavingsUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is SavingsUiState.Error ->
                    Text(state.message, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center))
                is SavingsUiState.Success ->
                    SavingsContent(
                        state = state,
                        onEditAccount = { accountToEdit = it },
                        onDeleteAccount = viewModel::deleteAccount,
                        onAddChild = { showAddChild = true },
                        onDeleteChild = viewModel::removeChild,
                        onAppliquerVersement = viewModel::appliquerVersement,
                        onAssocierComptes = { child, selectionnes ->
                            viewModel.associerComptesEnfant(
                                child,
                                (state as SavingsUiState.Success).accounts,
                                selectionnes
                            )
                        }
                    )
            }
        }
    }

    if (showAddSheet) {
        val children = (uiState as? SavingsUiState.Success)?.children ?: emptyList()
        AddSavingsSheet(
            children = children,
            onSave = { type, label, balance, contribution, currency, childId ->
                viewModel.saveAccount(type, label, balance, contribution, currency, childId)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Feuille d'édition : s'ouvre quand l'utilisateur tape le crayon sur une carte
    accountToEdit?.let { compte ->
        val children = (uiState as? SavingsUiState.Success)?.children ?: emptyList()
        EditSavingsSheet(
            account = compte,
            children = children,
            onSave = { type, label, balance, contribution, currency, childId ->
                viewModel.updateAccount(compte, type, label, balance, contribution, currency, childId)
            },
            onDismiss = { accountToEdit = null }
        )
    }

    if (showAddChild) {
        AddChildDialog(
            onConfirm = viewModel::addChild,
            onDismiss = { showAddChild = false }
        )
    }
}

@Composable
private fun SavingsContent(
    state: SavingsUiState.Success,
    onEditAccount: (SavingsAccount) -> Unit,
    onDeleteAccount: (SavingsAccount) -> Unit,
    onAddChild: () -> Unit,
    onDeleteChild: (Child) -> Unit,
    onAppliquerVersement: (SavingsAccount) -> Unit,
    onAssocierComptes: (Child, Set<Long>) -> Unit
) {
    val totalEpargne = state.accounts.sumOf { it.currentBalanceCents }
    val totalMensuel = state.accounts.sumOf { it.monthlyContributionCents }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Épargne", style = MaterialTheme.typography.headlineMedium)
        }

        // Résumé global
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total épargne", style = MaterialTheme.typography.labelMedium)
                        Text(totalEpargne.toCurrencyDisplay(Currency.EUR),
                            style = MaterialTheme.typography.titleLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Versements/mois", style = MaterialTheme.typography.labelMedium)
                        Text(totalMensuel.toCurrencyDisplay(Currency.EUR),
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // Comptes par type
        if (state.accounts.isNotEmpty()) {
            item { Text("Comptes", style = MaterialTheme.typography.titleMedium) }
            items(state.accounts, key = { "compte_${it.id}" }) { account ->
                SavingsAccountCard(
                    account = account,
                    childName = state.children.find { it.id == account.childId }?.name,
                    onEdit = { onEditAccount(account) },
                    onDelete = { onDeleteAccount(account) },
                    onVersement = { onAppliquerVersement(account) }
                )
            }
        }

        // Section enfants
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enfants", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddChild) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }

        if (state.children.isEmpty()) {
            item {
                Text("Aucun enfant enregistré.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(state.children, key = { "enfant_${it.id}" }) { child ->
                ChildCard(
                    child = child,
                    savingsAccounts = state.accounts.filter { it.childId == child.id },
                    tousLesComptes = state.accounts,
                    onDelete = { onDeleteChild(child) },
                    onAssocierComptes = { selectionnes -> onAssocierComptes(child, selectionnes) }
                )
            }
        }
    }
}

@Composable
private fun SavingsAccountCard(
    account: SavingsAccount,
    childName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVersement: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showVersementConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SavingsTypeChip(
                            type       = account.type,
                            customName = if (account.type == SavingsType.AUTRE) account.label else null
                        )
                        // Quand type == AUTRE, le label EST le nom du type — pas de doublon
                        if (account.type != SavingsType.AUTRE) {
                            Text(account.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (childName != null) {
                        Text("Pour $childName", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        "Solde : ${account.currentBalanceCents.toCurrencyDisplay(account.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (account.monthlyContributionCents > 0) {
                        Text(
                            "+ ${account.monthlyContributionCents.toCurrencyDisplay(account.currency)}/mois",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Crayon = modifier | Poubelle = supprimer
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Bouton versement : visible uniquement si un montant mensuel est configuré
            if (account.monthlyContributionCents > 0) {
                OutlinedButton(
                    onClick = { showVersementConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Versement du mois (+${account.monthlyContributionCents.toCurrencyDisplay(account.currency)})")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ce compte ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showVersementConfirm) {
        val dateAujourdhui = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
        AlertDialog(
            onDismissRequest = { showVersementConfirm = false },
            title = { Text("Appliquer le versement ?") },
            text = {
                Text(
                    "${account.monthlyContributionCents.toCurrencyDisplay(account.currency)} seront " +
                    "ajoutés au solde le $dateAujourdhui. Un seul versement par mois est autorisé."
                )
            },
            confirmButton = {
                TextButton(onClick = { onVersement(); showVersementConfirm = false }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showVersementConfirm = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun SavingsTypeChip(type: SavingsType, customName: String? = null) {
    val label = if (type == SavingsType.AUTRE && !customName.isNullOrBlank()) customName
                else type.displayName
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ChildCard(
    child: Child,
    savingsAccounts: List<SavingsAccount>,
    tousLesComptes: List<SavingsAccount>,
    onDelete: () -> Unit,
    onAssocierComptes: (Set<Long>) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showAssocier by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary)
                    Text(child.name, style = MaterialTheme.typography.titleSmall)
                }
                Row {
                    IconButton(onClick = { showAssocier = true }) {
                        Icon(Icons.Filled.Link, contentDescription = "Associer des comptes",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (savingsAccounts.isEmpty()) {
                Text("Aucun compte épargne associé", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                savingsAccounts.forEach { acc ->
                    Text(
                        "• ${acc.type.displayName} — ${acc.currentBalanceCents.toCurrencyDisplay(acc.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ${child.name} ?") },
            text = { Text("Les comptes épargne associés ne seront pas supprimés.") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showAssocier) {
        AssocierComptesDialog(
            childName = child.name,
            tousLesComptes = tousLesComptes,
            comptesDejaAssocies = savingsAccounts.map { it.id }.toSet(),
            onConfirm = { selectionnes -> onAssocierComptes(selectionnes); showAssocier = false },
            onDismiss = { showAssocier = false }
        )
    }
}

@Composable
private fun AssocierComptesDialog(
    childName: String,
    tousLesComptes: List<SavingsAccount>,
    comptesDejaAssocies: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    // État local : IDs des comptes cochés dans le dialog
    var selectionnes by remember { mutableStateOf(comptesDejaAssocies) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comptes de $childName") },
        text = {
            if (tousLesComptes.isEmpty()) {
                Text("Aucun compte épargne disponible.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tousLesComptes.forEach { compte ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = compte.id in selectionnes,
                                onCheckedChange = { coche ->
                                    selectionnes = if (coche) selectionnes + compte.id
                                    else selectionnes - compte.id
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val nomType = if (compte.type == SavingsType.AUTRE) compte.label
                                              else compte.type.displayName
                                Text(nomType, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    compte.currentBalanceCents.toCurrencyDisplay(compte.currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectionnes) }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSavingsSheet(
    children: List<Child>,
    onSave: (SavingsType, String, String, String, Currency, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(SavingsType.LIVRET_A) }
    var label by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(Currency.EUR) }
    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }

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
            Text("Nouveau compte épargne", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName, onValueChange = {},
                    readOnly = true, label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    SavingsType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                // AUTRE : vider le libellé pour forcer la saisie du nom personnalisé
                                // Autres types : pré-remplir avec le displayName si le champ est vide
                                if (type == SavingsType.AUTRE) label = ""
                                else if (label.isEmpty()) label = type.displayName
                                typeExpanded = false
                            })
                    }
                }
            }

            // Quand AUTRE : le libellé devient le nom du type personnalisé (obligatoire)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = {
                    Text(
                        if (selectedType == SavingsType.AUTRE) "Nom du type (ex. Nalo, PEP, Crowdfunding...)"
                        else "Libellé"
                    )
                },
                isError = selectedType == SavingsType.AUTRE && label.isBlank(),
                supportingText = if (selectedType == SavingsType.AUTRE && label.isBlank()) {
                    { Text("Le nom du type est obligatoire") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = balance, onValueChange = { balance = it },
                label = { Text("Solde actuel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = contribution, onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false })
                    }
                }
            }

            if (children.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = childExpanded, onExpandedChange = { childExpanded = it }) {
                    OutlinedTextField(
                        value = selectedChild?.name ?: "Aucun (personnel)", onValueChange = {},
                        readOnly = true, label = { Text("Associer à un enfant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(childExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = childExpanded, onDismissRequest = { childExpanded = false }) {
                        DropdownMenuItem(text = { Text("Aucun (personnel)") },
                            onClick = { selectedChild = null; childExpanded = false })
                        children.forEach { child ->
                            DropdownMenuItem(text = { Text(child.name) },
                                onClick = { selectedChild = child; childExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(selectedType, label, balance, contribution, selectedCurrency, selectedChild?.id) },
                enabled = label.isNotBlank() && balance.toDoubleOrNull()?.let { it >= 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter le compte") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSavingsSheet(
    account: SavingsAccount,
    children: List<Child>,
    onSave: (SavingsType, String, String, String, Currency, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Pré-remplissage avec les valeurs actuelles du compte
    var selectedType by remember { mutableStateOf(account.type) }
    var label by remember { mutableStateOf(account.label) }
    var balance by remember { mutableStateOf("%.2f".format(account.currentBalanceCents / 100.0)) }
    var contribution by remember {
        mutableStateOf(
            if (account.monthlyContributionCents > 0) "%.2f".format(account.monthlyContributionCents / 100.0) else ""
        )
    }
    var selectedCurrency by remember { mutableStateOf(account.currency) }
    var selectedChild by remember { mutableStateOf(children.find { it.id == account.childId }) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }

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
            Text("Modifier le compte épargne", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName, onValueChange = {},
                    readOnly = true, label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    SavingsType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) },
                            onClick = {
                                if (type == SavingsType.AUTRE && selectedType != SavingsType.AUTRE) label = ""
                                selectedType = type
                                typeExpanded = false
                            })
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = {
                    Text(
                        if (selectedType == SavingsType.AUTRE) "Nom du type (ex. Nalo, PEP, Crowdfunding...)"
                        else "Libellé"
                    )
                },
                isError = selectedType == SavingsType.AUTRE && label.isBlank(),
                supportingText = if (selectedType == SavingsType.AUTRE && label.isBlank()) {
                    { Text("Le nom du type est obligatoire") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = balance, onValueChange = { balance = it },
                label = { Text("Solde actuel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = contribution, onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false })
                    }
                }
            }

            if (children.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = childExpanded, onExpandedChange = { childExpanded = it }) {
                    OutlinedTextField(
                        value = selectedChild?.name ?: "Aucun (personnel)", onValueChange = {},
                        readOnly = true, label = { Text("Associer à un enfant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(childExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = childExpanded, onDismissRequest = { childExpanded = false }) {
                        DropdownMenuItem(text = { Text("Aucun (personnel)") },
                            onClick = { selectedChild = null; childExpanded = false })
                        children.forEach { child ->
                            DropdownMenuItem(text = { Text(child.name) },
                                onClick = { selectedChild = child; childExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(selectedType, label, balance, contribution, selectedCurrency, selectedChild?.id) },
                enabled = label.isNotBlank() && balance.toDoubleOrNull()?.let { it >= 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }
}

@Composable
private fun AddChildDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un enfant") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Prénom") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

