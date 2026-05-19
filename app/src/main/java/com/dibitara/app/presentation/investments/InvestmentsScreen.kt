package com.dibitara.app.presentation.investments

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
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.presentation.common.toCurrencyDisplay
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InvestmentsScreen(viewModel: InvestmentsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var showAddRealEstate by remember { mutableStateOf(false) }
    var showAddScpi      by remember { mutableStateOf(false) }
    var showAddAirbnb    by remember { mutableStateOf(false) }
    // Éléments en cours d'édition — null = pas d'édition ouverte
    var realEstateToEdit by remember { mutableStateOf<RealEstateAsset?>(null) }
    var scpiToEdit       by remember { mutableStateOf<ScpiInvestment?>(null) }
    var airbnbToEdit     by remember { mutableStateOf<AirbnbRental?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InvestmentsEvent.Saved -> {
                    showAddRealEstate = false
                    showAddScpi = false
                    showAddAirbnb = false
                    realEstateToEdit = null
                    scpiToEdit = null
                    airbnbToEdit = null
                    snackbarHostState.showSnackbar("Investissement enregistré")
                }
                is InvestmentsEvent.VersementApplique -> snackbarHostState.showSnackbar("Versement appliqué ✓")
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
                        onEditRealEstate = { realEstateToEdit = it },
                        onEditScpi = { scpiToEdit = it },
                        onEditAirbnb = { airbnbToEdit = it },
                        onDeleteRealEstate = viewModel::deleteRealEstate,
                        onDeleteScpi = viewModel::deleteScpi,
                        onDeleteAirbnb = viewModel::deleteAirbnb,
                        onAppliquerVersementScpi = viewModel::appliquerVersementScpi
                    )
            }
        }
    }

    if (showAddRealEstate) {
        AddRealEstateSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, value, currency -> viewModel.addRealEstate(label, value, currency) },
            onDismiss = { showAddRealEstate = false }
        )
    }
    if (showAddScpi) {
        AddScpiSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, shares, shareValue, contribution, currency ->
                viewModel.addScpi(label, shares, shareValue, contribution, currency)
            },
            onDismiss = { showAddScpi = false }
        )
    }
    if (showAddAirbnb) {
        AddAirbnbSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, amount, date, currency -> viewModel.addAirbnbRental(label, amount, date, currency) },
            onDismiss = { showAddAirbnb = false }
        )
    }

    // Sheets d'édition
    realEstateToEdit?.let { asset ->
        EditRealEstateSheet(
            asset = asset,
            onSave = { label, value, currency -> viewModel.updateRealEstate(asset, label, value, currency) },
            onDismiss = { realEstateToEdit = null }
        )
    }
    scpiToEdit?.let { scpi ->
        EditScpiSheet(
            scpi = scpi,
            onSave = { label, shares, shareValue, contribution, currency ->
                viewModel.updateScpi(scpi, label, shares, shareValue, contribution, currency)
            },
            onDismiss = { scpiToEdit = null }
        )
    }
    airbnbToEdit?.let { rental ->
        EditAirbnbSheet(
            rental = rental,
            onSave = { label, amount, currency -> viewModel.updateAirbnbRental(rental, label, amount, currency) },
            onDismiss = { airbnbToEdit = null }
        )
    }
}

@Composable
private fun InvestmentsContent(
    state: InvestmentsUiState.Success,
    onAddRealEstate: () -> Unit,
    onAddScpi: () -> Unit,
    onAddAirbnb: () -> Unit,
    onEditRealEstate: (RealEstateAsset) -> Unit,
    onEditScpi: (ScpiInvestment) -> Unit,
    onEditAirbnb: (AirbnbRental) -> Unit,
    onDeleteRealEstate: (RealEstateAsset) -> Unit,
    onDeleteScpi: (ScpiInvestment) -> Unit,
    onDeleteAirbnb: (AirbnbRental) -> Unit,
    onAppliquerVersementScpi: (ScpiInvestment) -> Unit
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

        // Graphique barres — affiché si au moins un actif immo ou SCPI
        if (state.realEstate.isNotEmpty() || state.scpi.isNotEmpty()) {
            item { AssetsBarChart(realEstate = state.realEstate, scpi = state.scpi) }
        }

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
                RealEstateCard(asset = asset, onEdit = { onEditRealEstate(asset) }, onDelete = { onDeleteRealEstate(asset) })
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
                ScpiCard(
                    scpi = scpi,
                    onEdit = { onEditScpi(scpi) },
                    onDelete = { onDeleteScpi(scpi) },
                    onVersement = { onAppliquerVersementScpi(scpi) }
                )
            }
        }

        // --- Section Revenus locatifs ---
        item {
            SectionHeader(title = "Revenus locatifs (${state.anneeLocatifs})", onAdd = onAddAirbnb)
        }
        if (state.airbnbRentals.isEmpty()) {
            item {
                EmptySectionText("Aucun revenu locatif enregistré.")
            }
        } else {
            items(state.airbnbRentals, key = { "airbnb_${it.id}" }) { rental ->
                AirbnbRentalCard(rental = rental, onEdit = { onEditAirbnb(rental) }, onDelete = { onDeleteAirbnb(rental) })
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
                    "Locatif / an",
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
private fun RealEstateCard(asset: RealEstateAsset, onEdit: () -> Unit, onDelete: () -> Unit) {
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
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                }
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                }
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
private fun ScpiCard(scpi: ScpiInvestment, onEdit: () -> Unit, onDelete: () -> Unit, onVersement: () -> Unit) {
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
            if (scpi.monthlyContributionCents > 0) {
                OutlinedButton(
                    onClick = { showVersementConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Versement du mois (+${scpi.monthlyContributionCents.toCurrencyDisplay(scpi.currency)})")
                }
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

    if (showVersementConfirm) {
        val dateAujourdhui = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
        AlertDialog(
            onDismissRequest = { showVersementConfirm = false },
            title = { Text("Appliquer le versement ?") },
            text = {
                Text(
                    "${scpi.monthlyContributionCents.toCurrencyDisplay(scpi.currency)} seront " +
                    "enregistrés comme versement SCPI le $dateAujourdhui. Un seul versement par mois est autorisé."
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
private fun AirbnbRentalCard(rental: AirbnbRental, onEdit: () -> Unit, onDelete: () -> Unit) {
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
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                }
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                }
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
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, value: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            Text("Nouveau bien immobilier", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé (ex. Appartement Lyon)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Valeur actuelle") },
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
                onClick = { onSave(label, value, selectedCurrency) },
                enabled = label.isNotBlank() && value.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScpiSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, shares: String, shareValue: String, contribution: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var shares by remember { mutableStateOf("") }
    var shareValue by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            Text("Nouvelle SCPI", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nom de la SCPI") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shares,
                    onValueChange = { shares = it },
                    label = { Text("Nb de parts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shareValue,
                    onValueChange = { shareValue = it },
                    label = { Text("Valeur / part") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = contribution,
                onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
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

            // Aperçu du total si les champs sont remplis
            val previewTotal = shares.toIntOrNull()?.let { s ->
                shareValue.replace(',', '.').toDoubleOrNull()?.let { v -> s * (v * 100).toLong() }
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
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, amount: String, date: LocalDate, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    // Le mois courant est utilisé par défaut — l'utilisateur entre les revenus du mois
    val today = remember { LocalDate.now() }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH)
    val focusManager = LocalFocusManager.current

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
            Text("Revenu locatif", style = MaterialTheme.typography.titleLarge)
            Text(
                "Mois : ${today.format(formatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Source (ex. Airbnb, Appartement Lyon...)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Revenu du mois") },
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
                onClick = { onSave(label, amount, today, selectedCurrency) },
                enabled = label.isNotBlank() && amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }
}

// ─── Bottom Sheets d'édition ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRealEstateSheet(
    asset: RealEstateAsset,
    onSave: (label: String, value: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(asset.label) }
    var value by remember { mutableStateOf("%.2f".format(asset.currentValueCents / 100.0).replace(',', '.')) }
    var selectedCurrency by remember { mutableStateOf(asset.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            Text("Modifier le bien immobilier", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text("Libellé") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = value, onValueChange = { value = it },
                label = { Text("Valeur actuelle") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

            Button(
                onClick = { onSave(label, value, selectedCurrency) },
                enabled = label.isNotBlank() && value.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScpiSheet(
    scpi: ScpiInvestment,
    onSave: (label: String, shares: String, shareValue: String, contribution: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(scpi.label) }
    var shares by remember { mutableStateOf(scpi.sharesCount.toString()) }
    var shareValue by remember { mutableStateOf("%.2f".format(scpi.shareValueCents / 100.0).replace(',', '.')) }
    var contribution by remember {
        mutableStateOf(if (scpi.monthlyContributionCents > 0) "%.2f".format(scpi.monthlyContributionCents / 100.0).replace(',', '.') else "")
    }
    var selectedCurrency by remember { mutableStateOf(scpi.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            Text("Modifier la SCPI", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text("Nom de la SCPI") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = shares, onValueChange = { shares = it },
                    label = { Text("Nb de parts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = shareValue, onValueChange = { shareValue = it },
                    label = { Text("Valeur / part") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = contribution, onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

            val previewTotal = shares.toIntOrNull()?.let { s ->
                shareValue.replace(',', '.').toDoubleOrNull()?.let { v -> s * (v * 100).toLong() }
            }
            if (previewTotal != null) {
                Text("Total estimé : ${previewTotal.toCurrencyDisplay(selectedCurrency)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }

            Button(
                onClick = { onSave(label, shares, shareValue, contribution, selectedCurrency) },
                enabled = label.isNotBlank() && shares.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAirbnbSheet(
    rental: AirbnbRental,
    onSave: (label: String, amount: String, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(rental.propertyLabel) }
    var amount by remember { mutableStateOf("%.2f".format(rental.amountCents / 100.0).replace(',', '.')) }
    var selectedCurrency by remember { mutableStateOf(rental.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH)
    val focusManager = LocalFocusManager.current

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
            Text("Modifier le revenu locatif", style = MaterialTheme.typography.titleLarge)
            Text(
                "Mois : ${rental.date.format(formatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text("Source (ex. Airbnb, Appartement)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("Revenu du mois") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

            Button(
                onClick = { onSave(label, amount, selectedCurrency) },
                enabled = label.isNotBlank() && amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }
}

// ─── Graphique barres ────────────────────────────────────────────────────────

@Composable
private fun AssetsBarChart(
    realEstate: List<RealEstateAsset>,
    scpi: List<ScpiInvestment>
) {
    // On combine immobilier et SCPI en une liste (libellé, valeur en centimes)
    val actifs = (realEstate.map { it.label to it.currentValueCents } +
                  scpi.map { it.label to it.totalValueCents })
        .filter { it.second > 0 }

    if (actifs.isEmpty()) return

    // ChartEntryModelProducer gère les mises à jour asynchrones des données du graphique
    val producer = remember { ChartEntryModelProducer() }
    // Libellés tronqués à 8 caractères pour tenir sur l'axe
    val labels = actifs.map { it.first.take(8) }

    LaunchedEffect(actifs) {
        // Conversion centimes → euros, x = index de l'actif dans la liste
        producer.setEntries(
            actifs.mapIndexed { i, (_, valueCents) ->
                entryOf(i.toFloat(), valueCents.toFloat() / 100f)
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Valeur par actif (€)", style = MaterialTheme.typography.titleMedium)
            ProvideChartStyle(m3ChartStyle()) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = producer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                            labels.getOrElse(value.toInt()) { "" }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

