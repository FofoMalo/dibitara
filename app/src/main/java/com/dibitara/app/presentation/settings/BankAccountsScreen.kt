package com.dibitara.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.presentation.common.chartColor
import com.dibitara.app.presentation.common.chartIcon
import com.dibitara.app.presentation.common.toCurrencyDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: (Long) -> Unit = {},
    viewModel: BankAccountsViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val comptes = summary.comptes
    var compteEnEdition by remember { mutableStateOf<BankAccount?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var compteASupprimer by remember { mutableStateOf<BankAccount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes comptes bancaires") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un compte")
            }
        }
    ) { padding ->
        if (comptes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun compte pour l'instant. Ajoutez-en un avec le bouton +.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comptes, key = { it.id }) { compte ->
                    BankAccountRow(
                        compte = compte,
                        onClick = { onNavigateToTransactions(compte.id) },
                        onEdit = { compteEnEdition = compte },
                        onDelete = { compteASupprimer = compte }
                    )
                }
                if (comptes.size > 1) {
                    item(key = "total") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(
                                summary.totalCents.toCurrencyDisplay(summary.currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        BankAccountDialog(
            compteExistant = null,
            onConfirm = { provider, label, solde, currency ->
                viewModel.sauvegarder(null, provider, label, solde, currency)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    compteEnEdition?.let { compte ->
        BankAccountDialog(
            compteExistant = compte,
            onConfirm = { provider, label, solde, currency ->
                viewModel.sauvegarder(compte, provider, label, solde, currency)
                compteEnEdition = null
            },
            onDismiss = { compteEnEdition = null }
        )
    }

    compteASupprimer?.let { compte ->
        AlertDialog(
            onDismissRequest = { compteASupprimer = null },
            title = { Text("Supprimer ce compte ?") },
            text = { Text("« ${compte.label} » sera supprimé. Les transactions déjà importées ne sont pas affectées.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.supprimer(compte)
                    compteASupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { compteASupprimer = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun BankAccountRow(
    compte: BankAccount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                compte.provider.chartIcon(),
                contentDescription = null,
                tint = compte.provider.chartColor()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(compte.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    compte.provider.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                compte.currentBalanceCents.toCurrencyDisplay(compte.currency),
                style = MaterialTheme.typography.bodyLarge
            )
            // Menu "..." plutôt que deux IconButton côte à côte (convention CLAUDE.md) :
            // l'icône de banque + le solde ne laissent pas assez de place pour deux boutons.
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankAccountDialog(
    compteExistant: BankAccount?,
    onConfirm: (provider: BankProvider, label: String, soldeStr: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(compteExistant?.provider ?: BankProvider.BRED) }
    var label by remember { mutableStateOf(compteExistant?.label ?: "") }
    var solde by remember {
        mutableStateOf(compteExistant?.let { "%.2f".format(it.currentBalanceCents / 100.0).replace(',', '.') } ?: "")
    }
    var selectedCurrency by remember { mutableStateOf(compteExistant?.currency ?: Currency.EUR) }
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedCurrency by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (compteExistant != null) "Modifier le compte" else "Nouveau compte") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.imePadding()) {
                ExposedDropdownMenuBox(expanded = expandedProvider, onExpandedChange = { expandedProvider = it }) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Banque") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedProvider) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedProvider, onDismissRequest = { expandedProvider = false }) {
                        BankProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = { selectedProvider = provider; expandedProvider = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Libellé") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = solde,
                    onValueChange = { solde = it },
                    label = { Text("Solde actuel") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = expandedCurrency, onExpandedChange = { expandedCurrency = it }) {
                    OutlinedTextField(
                        value = "${selectedCurrency.name} (${selectedCurrency.symbol})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Devise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCurrency) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedCurrency, onDismissRequest = { expandedCurrency = false }) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.name} (${currency.symbol})") },
                                onClick = { selectedCurrency = currency; expandedCurrency = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProvider, label, solde, selectedCurrency) },
                enabled = label.isNotBlank() && solde.replace(',', '.').toDoubleOrNull() != null
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
