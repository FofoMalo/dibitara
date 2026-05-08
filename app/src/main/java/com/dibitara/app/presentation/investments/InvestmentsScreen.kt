package com.dibitara.app.presentation.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InvestmentsScreen(viewModel: InvestmentsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddRealEstate by remember { mutableStateOf(false) }
    var showAddScpi by remember { mutableStateOf(false) }
    var showAddAirbnb by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InvestmentsEvent.Saved -> {
                    showAddRealEstate = false
                    showAddScpi = false
                    showAddAirbnb = false
                    snackbarHostState.showSnackbar("Investissement enregistré")
                }
                is InvestmentsEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is InvestmentsUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is InvestmentsUiState.Error ->
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                is InvestmentsUiState.Success ->
                    InvestmentsContent(
                        state = state,
                        onAddRealEstate = { showAddRealEstate = true },
                        onAddScpi = { showAddScpi = true },
                        onAddAirbnb = { showAddAirbnb = true },
                        onDeleteRealEstate = viewModel::deleteRealEstate,
                        onDeleteScpi = viewModel::deleteScpi,
                        onDeleteAirbnb = viewModel::deleteAirbnb
                    )
            }
        }
    }

    if (showAddRealEstate) {
        AddRealEstateSheet(
            onSave = { label, value, currency -> viewModel.addRealEstate(label, value, currency) },
            onDismiss = { showAddRealEstate = false }
        )
    }

    if (showAddScpi) {
        AddScpiSheet(
            onSave = { label, shares, shareValue, contribution, currency ->
                viewModel.addScpi(label, shares, shareValue, contribution, currency)
            },
            onDismiss = { showAddScpi = false }
        )
    }

    if (showAddAirbnb) {
        AddAirbnbSheet(
            onSave = { label, amount, date, currency -> viewModel.addAirbnbRental(label, amount, date, currency) },
            onDismiss = { showAddAirbnb = false }
        )
    }
}

@Composable
private fun InvestmentsContent(
    state: InvestmentsUiState.Success,
    onAddRealEstate: () -> Unit,
    onAddScpi: () -> Unit,
    onAddAirbnb: () -> Unit,
    onDeleteRealEstate: (RealEstateAsset) -> Unit,
    onDeleteScpi: (ScpiInvestment) -> Unit,
    onDeleteAirbnb: (AirbnbRental) -> Unit
) {
    val totalCents = state.realEstate.sumOf { it.currentValueCents } +
            state.scpi.sumOf { it.totalValueCents }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Investissements", style = MaterialTheme.typography.headlineMedium) }

        // Carte récapitulative
        item { TotalInvestmentsCard(totalCents = totalCents, airbnbAnnualCents = state.airbnbAnnualTotal) }

        // --- Section Immobilier ---
        item {
            SectionHeader(title = "Immobilier", onAdd = onAddRealEstate)
        }
        if (state.realEstate.isEmpty()) {
            item {
                EmptySectionText("Aucun bien immobilier enregistré.")
            }
        } else {
            items(state.realEstate, key = { "immo_${it.id}" }) { asset ->
                RealEstateCard(asset = asset, onDelete = { onDeleteRealEstate(asset) })
            }
        }

        // --- Section SCPI ---
        item {
            SectionHeader(title = "SCPI", onAdd = onAddScpi)
        }
        if (state.scpi.isEmpty()) {
            item {
                EmptySectionText("Aucune SCPI enregistrée.")
            }
        } else {
            items(state.scpi, key = { "scpi_${it.id}" }) { scpi ->
                ScpiCard(scpi = scpi, onDelete = { onDeleteScpi(scpi) })
            }
        }

        // --- Section Revenus Airbnb ---
        item {
            SectionHeader(title = "Revenus Airbnb (année)", onAdd = onAddAirbnb)
        }
        if (state.airbnbRentals.isEmpty()) {
            item {
                EmptySectionText("Aucun revenu Airbnb enregistré.")
            }
        } else {
            items(state.airbnbRentals, key = { "airbnb_${it.id}" }) { rental ->
                AirbnbRentalCard(rental = rental, onDelete = { onDeleteAirbnb(rental) })
            }
        }
    }
}

@Composable
private fun TotalInvestmentsCard(totalCents: Long, airbnbAnnualCents: Long) {
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
                    "Valeur totale",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    totalCents.toCurrencyDisplay(Currency.EUR),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Airbnb / an",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    airbnbAnnualCents.toCurrencyDisplay(Currency.EUR),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Ajouter")
        }
    }
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RealEstateCard(asset: RealEstateAsset, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Valeur actuelle : ${asset.currentValueCents.toCurrencyDisplay(asset.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Mis à jour le ${asset.updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ce bien ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun ScpiCard(scpi: ScpiInvestment, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(scpi.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${scpi.sharesCount} parts × ${scpi.shareValueCents.toCurrencyDisplay(scpi.currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Total : ${scpi.totalValueCents.toCurrencyDisplay(scpi.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (scpi.monthlyContributionCents > 0) {
                    Text(
                        "+ ${scpi.monthlyContributionCents.toCurrencyDisplay(scpi.currency)}/mois",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer cette SCPI ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun AirbnbRentalCard(rental: AirbnbRental, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rental.propertyLabel, style = MaterialTheme.typography.bodyLarge)
                Text(
                    rental.date.format(formatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    rental.amountCents.toCurrencyDisplay(rental.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ce revenu ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }
}

// ─── Bottom Sheets d'ajout ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRealEstateSheet(
    onSave: (label: String, value: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(Currency.EUR) }
    var currencyExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nouveau bien immobilier", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé (ex. Appartement Lyon)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Valeur actuelle") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                onClick = { onSave(label, value, selectedCurrency) },
                enabled = label.isNotBlank() && value.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScpiSheet(
    onSave: (label: String, shares: String, shareValue: String, contribution: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var shares by remember { mutableStateOf("") }
    var shareValue by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(Currency.EUR) }
    var currencyExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nouvelle SCPI", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nom de la SCPI") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shares,
                    onValueChange = { shares = it },
                    label = { Text("Nb de parts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shareValue,
                    onValueChange = { shareValue = it },
                    label = { Text("Valeur / part") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = contribution,
                onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            // Aperçu du total si les champs sont remplis
            val previewTotal = shares.toIntOrNull()?.let { s ->
                shareValue.toDoubleOrNull()?.let { v -> s * (v * 100).toLong() }
            }
            if (previewTotal != null) {
                Text(
                    "Total estimé : ${previewTotal.toCurrencyDisplay(selectedCurrency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Button(
                onClick = { onSave(label, shares, shareValue, contribution, selectedCurrency) },
                enabled = label.isNotBlank() && shares.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAirbnbSheet(
    onSave: (label: String, amount: String, date: LocalDate, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(Currency.EUR) }
    var currencyExpanded by remember { mutableStateOf(false) }
    // Le mois courant est utilisé par défaut — l'utilisateur entre les revenus du mois
    val today = remember { LocalDate.now() }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Revenu Airbnb", style = MaterialTheme.typography.titleLarge)
            Text(
                "Mois : ${today.format(formatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Bien loué (ex. Studio Bordeaux)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Revenu du mois") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                onClick = { onSave(label, amount, today, selectedCurrency) },
                enabled = label.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

