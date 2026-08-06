package com.dibitara.app.presentation.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import kotlin.math.roundToLong
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.EmployeeSavingsType
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.presentation.common.HorizontalBarChart
import com.dibitara.app.presentation.common.HorizontalBarEntry
import com.dibitara.app.presentation.common.TrendChip
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InvestmentsScreen(viewModel: InvestmentsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var showAddRealEstate    by remember { mutableStateOf(false) }
    var showAddScpi          by remember { mutableStateOf(false) }
    var showAddAirbnb        by remember { mutableStateOf(false) }
    var showAddVehicle       by remember { mutableStateOf(false) }
    var showAddCustomAsset   by remember { mutableStateOf(false) }
    var showAddEmpSavings    by remember { mutableStateOf(false) }
    // Éléments en cours d'édition - null = pas d'édition ouverte
    var realEstateToEdit  by remember { mutableStateOf<RealEstateAsset?>(null) }
    var scpiToEdit        by remember { mutableStateOf<ScpiInvestment?>(null) }
    var airbnbToEdit      by remember { mutableStateOf<AirbnbRental?>(null) }
    var vehicleToEdit     by remember { mutableStateOf<VehicleRentalEntry?>(null) }
    var customAssetToEdit by remember { mutableStateOf<CustomAsset?>(null) }
    var empSavingsToEdit  by remember { mutableStateOf<EmployeeSavings?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InvestmentsEvent.Saved -> {
                    showAddRealEstate = false; showAddScpi = false; showAddAirbnb = false; showAddVehicle = false
                    showAddCustomAsset = false; showAddEmpSavings = false
                    realEstateToEdit = null; scpiToEdit = null; airbnbToEdit = null; vehicleToEdit = null
                    customAssetToEdit = null; empSavingsToEdit = null
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
                        onAddVehicle = { showAddVehicle = true },
                        onAddCustomAsset = { showAddCustomAsset = true },
                        onAddEmpSavings = { showAddEmpSavings = true },
                        onEditRealEstate = { realEstateToEdit = it },
                        onEditScpi = { scpiToEdit = it },
                        onEditAirbnb = { airbnbToEdit = it },
                        onEditVehicle = { vehicleToEdit = it },
                        onEditCustomAsset = { customAssetToEdit = it },
                        onEditEmpSavings = { empSavingsToEdit = it },
                        onDeleteRealEstate = viewModel::deleteRealEstate,
                        onDeleteScpi = viewModel::deleteScpi,
                        onDeleteAirbnb = viewModel::deleteAirbnb,
                        onDeleteVehicle = viewModel::deleteVehicleRentalEntry,
                        onDeleteCustomAsset = viewModel::deleteCustomAsset,
                        onDeleteEmpSavings = viewModel::deleteEmployeeSavings,
                        onAppliquerVersementScpi = viewModel::appliquerVersementScpi
                    )
            }
        }
    }

    if (showAddCustomAsset) {
        AddCustomAssetSheet(defaultCurrency = defaultCurrency,
            onSave = { label, value, cur -> viewModel.addCustomAsset(label, value, cur) },
            onDismiss = { showAddCustomAsset = false })
    }
    if (showAddEmpSavings) {
        AddEmployeeSavingsSheet(defaultCurrency = defaultCurrency,
            onSave = { type, label, balance, contrib, cur -> viewModel.addEmployeeSavings(type, label, balance, contrib, cur) },
            onDismiss = { showAddEmpSavings = false })
    }
    customAssetToEdit?.let { asset ->
        EditCustomAssetSheet(asset = asset,
            onSave = { label, value, cur -> viewModel.updateCustomAsset(asset, label, value, cur) },
            onDismiss = { customAssetToEdit = null })
    }
    empSavingsToEdit?.let { savings ->
        EditEmployeeSavingsSheet(savings = savings,
            onSave = { type, label, balance, contrib, cur -> viewModel.updateEmployeeSavings(savings, type, label, balance, contrib, cur) },
            onDismiss = { empSavingsToEdit = null })
    }
    if (showAddRealEstate) {
        val debts = (uiState as? InvestmentsUiState.Success)?.availableDebts ?: emptyList()
        AddRealEstateSheet(
            defaultCurrency = defaultCurrency,
            availableDebts  = debts,
            onSave = { label, value, currency, debtId ->
                viewModel.addRealEstate(label, value, currency, debtId)
            },
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
    if (showAddVehicle) {
        AddVehicleRentalSheet(
            defaultCurrency = defaultCurrency,
            onSave = { label, amount, type, date, currency -> viewModel.addVehicleRentalEntry(label, amount, type, date, currency) },
            onDismiss = { showAddVehicle = false }
        )
    }

    // Sheets d'édition
    realEstateToEdit?.let { asset ->
        val debts = (uiState as? InvestmentsUiState.Success)?.availableDebts ?: emptyList()
        EditRealEstateSheet(
            asset          = asset,
            availableDebts = debts,
            onSave = { label, value, currency, debtId ->
                viewModel.updateRealEstate(asset, label, value, currency, debtId)
            },
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
    vehicleToEdit?.let { entry ->
        EditVehicleRentalSheet(
            entry = entry,
            onSave = { label, amount, type, date, currency -> viewModel.updateVehicleRentalEntry(entry, label, amount, type, date, currency) },
            onDismiss = { vehicleToEdit = null }
        )
    }
}

@Composable
private fun InvestmentsContent(
    state: InvestmentsUiState.Success,
    onAddRealEstate: () -> Unit,
    onAddScpi: () -> Unit,
    onAddAirbnb: () -> Unit,
    onAddVehicle: () -> Unit,
    onAddCustomAsset: () -> Unit,
    onAddEmpSavings: () -> Unit,
    onEditRealEstate: (RealEstateAsset) -> Unit,
    onEditScpi: (ScpiInvestment) -> Unit,
    onEditAirbnb: (AirbnbRental) -> Unit,
    onEditVehicle: (VehicleRentalEntry) -> Unit,
    onEditCustomAsset: (CustomAsset) -> Unit,
    onEditEmpSavings: (EmployeeSavings) -> Unit,
    onDeleteRealEstate: (RealEstateAsset) -> Unit,
    onDeleteScpi: (ScpiInvestment) -> Unit,
    onDeleteAirbnb: (AirbnbRental) -> Unit,
    onDeleteVehicle: (VehicleRentalEntry) -> Unit,
    onDeleteCustomAsset: (CustomAsset) -> Unit,
    onDeleteEmpSavings: (EmployeeSavings) -> Unit,
    onAppliquerVersementScpi: (ScpiInvestment) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Investissements", style = MaterialTheme.typography.headlineMedium) }

        // Carte récapitulative
        item { TotalInvestmentsCard(
            totalCents       = state.totalInvestmentsCents,
            airbnbAnnualCents = state.airbnbAnnualTotal,
            currency         = state.summaryCurrency,
            trendPct         = state.patrimoineTrendPct
        ) }

        // Graphique barres - affiché si au moins un actif immo ou SCPI
        if (state.realEstate.isNotEmpty() || state.scpi.isNotEmpty()) {
            item {
                AssetsBarChart(
                    realEstate     = state.realEstate,
                    scpi           = state.scpi,
                    targetCurrency = state.summaryCurrency,
                    rates          = state.rates
                )
            }
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
                val linkedDebt = asset.debtId?.let { id -> state.availableDebts.find { it.id == id } }
                RealEstateCard(
                    asset       = asset,
                    linkedDebt  = linkedDebt,
                    rates       = state.rates,
                    onEdit      = { onEditRealEstate(asset) },
                    onDelete    = { onDeleteRealEstate(asset) }
                )
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
            item { EmptySectionText("Aucun revenu locatif enregistré.") }
        } else {
            items(state.airbnbRentals, key = { "airbnb_${it.id}" }) { rental ->
                AirbnbRentalCard(rental = rental, onEdit = { onEditAirbnb(rental) }, onDelete = { onDeleteAirbnb(rental) })
            }
        }

        // --- Section Véhicule locatif ---
        // Indépendante d'Airbnb : cumulée depuis le début de l'activité (pas filtrée par année),
        // avec une carte de synthèse séparée puisque revenus ET charges sont suivis ici.
        item { SectionHeader(title = "Véhicule locatif", onAdd = onAddVehicle) }
        if (state.vehicleRentalEntries.isEmpty()) {
            item { EmptySectionText("Aucune entrée enregistrée pour le véhicule locatif.") }
        } else {
            item {
                VehicleRentalSummaryCard(
                    revenueCents = state.vehicleRentalRevenueCents,
                    chargeCents  = state.vehicleRentalChargeCents,
                    currency     = state.summaryCurrency
                )
            }
            items(state.vehicleRentalEntries, key = { "vehicle_${it.id}" }) { entry ->
                VehicleRentalEntryCard(entry = entry, onEdit = { onEditVehicle(entry) }, onDelete = { onDeleteVehicle(entry) })
            }
        }

        // --- Section Actifs libres ---
        item { SectionHeader(title = "Actifs libres", onAdd = onAddCustomAsset) }
        if (state.customAssets.isEmpty()) {
            item { EmptySectionText("Aucun actif libre enregistré (crypto, actions, œuvres...).") }
        } else {
            items(state.customAssets, key = { "custom_${it.id}" }) { asset ->
                CustomAssetCard(asset = asset, onEdit = { onEditCustomAsset(asset) }, onDelete = { onDeleteCustomAsset(asset) })
            }
        }

        // --- Section Épargne salariale ---
        item { SectionHeader(title = "Épargne salariale", onAdd = onAddEmpSavings) }
        if (state.employeeSavings.isEmpty()) {
            item { EmptySectionText("Aucun plan d'épargne salariale enregistré (PEE, PERCO).") }
        } else {
            items(state.employeeSavings, key = { "emp_${it.id}" }) { savings ->
                EmployeeSavingsCard(savings = savings, onEdit = { onEditEmpSavings(savings) }, onDelete = { onDeleteEmpSavings(savings) })
            }
        }
    }
}

@Composable
private fun TotalInvestmentsCard(totalCents: Long, airbnbAnnualCents: Long, currency: Currency, trendPct: Float? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Valeur totale",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    if (trendPct != null) TrendChip(trendPct)
                }
                Text(
                    totalCents.toCurrencyDisplay(currency),
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
                    airbnbAnnualCents.toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun VehicleRentalSummaryCard(revenueCents: Long, chargeCents: Long, currency: Currency) {
    val netCents = revenueCents - chargeCents
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Revenus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    revenueCents.toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column {
                Text(
                    "Charges",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    chargeCents.toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Net",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    netCents.toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    // error réservé au cas net négatif, jamais utilisé pour signaler une simple charge
                    color = if (netCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
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
private fun RealEstateCard(
    asset      : RealEstateAsset,
    linkedDebt : com.dibitara.app.domain.model.Debt?,
    rates      : ExchangeRates,
    onEdit     : () -> Unit,
    onDelete   : () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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
                if (linkedDebt != null) {
                    Text(
                        "Crédit lié : ${linkedDebt.label} - −${linkedDebt.totalCents.toCurrencyDisplay(linkedDebt.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    // Conversion nécessaire : le crédit lié peut être dans une devise différente du bien
                    val detteDansDeviseDuBien = CurrencyConverter.convertCents(
                        linkedDebt.totalCents, linkedDebt.currency, asset.currency, rates
                    )
                    val equite = asset.currentValueCents - detteDansDeviseDuBien
                    Text(
                        "Équité nette : ${equite.toCurrencyDisplay(asset.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (equite >= 0) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "Mis à jour le ${asset.updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showConfirm = true }
                    )
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
    var showMenu by remember { mutableStateOf(false) }

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
                        "${if (scpi.sharesCount % 1.0 == 0.0) scpi.sharesCount.toInt().toString() else scpi.sharesCount.toString()} parts × ${scpi.shareValueCents.toCurrencyDisplay(scpi.currency)}",
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
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showConfirm = true }
                        )
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
    var showMenu by remember { mutableStateOf(false) }
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
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showConfirm = true }
                    )
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

@Composable
private fun VehicleRentalEntryCard(entry: VehicleRentalEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val isRevenu = entry.entryType == VehicleEntryType.REVENU

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.date.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(entry.entryType.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isRevenu) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            labelColor     = if (isRevenu) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                Text(
                    "${if (isRevenu) "+" else "-"}${entry.amountCents.toCurrencyDisplay(entry.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRevenu) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showConfirm = true }
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer cette entrée ?") },
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
    availableDebts : List<Debt> = emptyList(),
    onSave: (label: String, value: String, currency: Currency, debtId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var selectedDebtId by remember { mutableStateOf<Long?>(null) }
    var debtExpanded by remember { mutableStateOf(false) }
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

            // Liaison à un crédit existant (optionnelle)
            ExposedDropdownMenuBox(expanded = debtExpanded, onExpandedChange = { debtExpanded = it }) {
                OutlinedTextField(
                    value = availableDebts.find { it.id == selectedDebtId }?.label ?: "Aucun",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Crédit lié (optionnel)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(debtExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = debtExpanded, onDismissRequest = { debtExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Aucun") },
                        onClick = { selectedDebtId = null; debtExpanded = false }
                    )
                    availableDebts.forEach { debt ->
                        DropdownMenuItem(
                            text = { Text(debt.label) },
                            onClick = { selectedDebtId = debt.id; debtExpanded = false }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(label, value, selectedCurrency, selectedDebtId) },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
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
            val previewTotal = shares.replace(',', '.').toDoubleOrNull()?.let { s ->
                shareValue.replace(',', '.').toDoubleOrNull()?.let { v -> (s * v * 100).roundToLong() }
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
                enabled = label.isNotBlank() && shares.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true,
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
    // Le mois courant est utilisé par défaut - l'utilisateur entre les revenus du mois
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleRentalSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (label: String, amount: String, type: VehicleEntryType, date: LocalDate, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VehicleEntryType.REVENU) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
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
            Text("Véhicule locatif", style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VehicleEntryType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = VehicleEntryType.entries.size)
                    ) { Text(type.displayName) }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(if (selectedType == VehicleEntryType.REVENU) "Source (ex. Location weekend)" else "Nature (ex. Entretien, Assurance)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = selectedDate.format(dateFormatter),
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
                onClick = { onSave(label, amount, selectedType, selectedDate, selectedCurrency) },
                enabled = label.isNotBlank() && amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
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
}

// ─── Bottom Sheets d'édition ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRealEstateSheet(
    asset          : RealEstateAsset,
    availableDebts : List<Debt> = emptyList(),
    onSave: (label: String, value: String, currency: Currency, debtId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(asset.label) }
    var value by remember { mutableStateOf("%.2f".format(asset.currentValueCents / 100.0).replace(',', '.')) }
    var selectedCurrency by remember { mutableStateOf(asset.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    // Pré-sélectionne le crédit déjà rattaché au bien, le cas échéant
    var selectedDebtId by remember { mutableStateOf(asset.debtId) }
    var debtExpanded by remember { mutableStateOf(false) }
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

            // Liaison à un crédit existant (optionnelle - pré-remplie si déjà rattaché)
            ExposedDropdownMenuBox(expanded = debtExpanded, onExpandedChange = { debtExpanded = it }) {
                OutlinedTextField(
                    value = availableDebts.find { it.id == selectedDebtId }?.label ?: "Aucun",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Crédit lié (optionnel)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(debtExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = debtExpanded, onDismissRequest = { debtExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Aucun") },
                        onClick = { selectedDebtId = null; debtExpanded = false }
                    )
                    availableDebts.forEach { debt ->
                        DropdownMenuItem(
                            text = { Text(debt.label) },
                            onClick = { selectedDebtId = debt.id; debtExpanded = false }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(label, value, selectedCurrency, selectedDebtId) },
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
    // Affiche "2" pour 2.0 parts, "2.2" pour 2.2 parts
    var shares by remember { mutableStateOf(if (scpi.sharesCount % 1.0 == 0.0) scpi.sharesCount.toInt().toString() else scpi.sharesCount.toString()) }
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
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

            val previewTotal = shares.replace(',', '.').toDoubleOrNull()?.let { s ->
                shareValue.replace(',', '.').toDoubleOrNull()?.let { v -> (s * v * 100).roundToLong() }
            }
            if (previewTotal != null) {
                Text("Total estimé : ${previewTotal.toCurrencyDisplay(selectedCurrency)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }

            Button(
                onClick = { onSave(label, shares, shareValue, contribution, selectedCurrency) },
                enabled = label.isNotBlank() && shares.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVehicleRentalSheet(
    entry: VehicleRentalEntry,
    onSave: (label: String, amount: String, type: VehicleEntryType, date: LocalDate, currency: Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(entry.label) }
    var amount by remember { mutableStateOf("%.2f".format(entry.amountCents / 100.0).replace(',', '.')) }
    var selectedType by remember { mutableStateOf(entry.entryType) }
    var selectedDate by remember { mutableStateOf(entry.date) }
    var selectedCurrency by remember { mutableStateOf(entry.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
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
            Text("Modifier l'entrée véhicule", style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VehicleEntryType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = VehicleEntryType.entries.size)
                    ) { Text(type.displayName) }
                }
            }

            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text(if (selectedType == VehicleEntryType.REVENU) "Source (ex. Location weekend)" else "Nature (ex. Entretien, Assurance)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("Montant") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = selectedDate.format(dateFormatter),
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
                onClick = { onSave(label, amount, selectedType, selectedDate, selectedCurrency) },
                enabled = label.isNotBlank() && amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
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
}

// ─── Cartes investissements personnalisés ─────────────────────────────────────

@Composable
private fun CustomAssetCard(asset: CustomAsset, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.label, style = MaterialTheme.typography.bodyLarge)
                Text(asset.totalValueCents.toCurrencyDisplay(asset.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                Text("Mis à jour le ${asset.updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showConfirm = true })
                }
            }
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("Supprimer cet actif ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } })
    }
}

@Composable
private fun EmployeeSavingsCard(savings: EmployeeSavings, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${savings.type.displayName} - ${savings.label}", style = MaterialTheme.typography.bodyLarge)
                Text("Solde : ${savings.currentBalanceCents.toCurrencyDisplay(savings.currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                if (savings.employerContributionCents > 0) {
                    Text("Abondement : ${savings.employerContributionCents.toCurrencyDisplay(savings.currency)}/mois", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showConfirm = true })
                }
            }
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("Supprimer ce plan ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } })
    }
}

// ─── Bottom Sheets investissements personnalisés ──────────────────────────────

@Composable
private fun EmployeeSavingsTypeSelector(selected: EmployeeSavingsType, onSelect: (EmployeeSavingsType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EmployeeSavingsType.entries.forEach { type ->
            FilterChip(selected = selected == type, onClick = { onSelect(type) }, label = { Text(type.name) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomAssetSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (String, String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Nouvel actif libre", style = MaterialTheme.typography.titleLarge)
            Text("Crypto, actions, œuvres d'art…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé (ex. Bitcoin, ETF Monde...)") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Valeur actuelle") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {}, readOnly = true, label = { Text("Devise") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) { Currency.entries.forEach { c -> DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") }, onClick = { selectedCurrency = c; currencyExpanded = false }) } }
            }
            Button(onClick = { onSave(label, value, selectedCurrency) }, enabled = label.isNotBlank() && value.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, modifier = Modifier.fillMaxWidth()) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCustomAssetSheet(
    asset: CustomAsset,
    onSave: (String, String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(asset.label) }
    var value by remember { mutableStateOf("%.2f".format(asset.totalValueCents / 100.0).replace(',', '.')) }
    var selectedCurrency by remember { mutableStateOf(asset.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Modifier l'actif libre", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Valeur actuelle") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {}, readOnly = true, label = { Text("Devise") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) { Currency.entries.forEach { c -> DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") }, onClick = { selectedCurrency = c; currencyExpanded = false }) } }
            }
            Button(onClick = { onSave(label, value, selectedCurrency) }, enabled = label.isNotBlank() && value.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer les modifications") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEmployeeSavingsSheet(
    defaultCurrency: Currency = Currency.EUR,
    onSave: (EmployeeSavingsType, String, String, String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var savingsType by remember { mutableStateOf(EmployeeSavingsType.PEE) }
    var label by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Nouvelle épargne salariale", style = MaterialTheme.typography.titleLarge)
            EmployeeSavingsTypeSelector(selected = savingsType, onSelect = { savingsType = it })
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé (ex. PEE Société Générale)") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Solde actuel") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contribution, onValueChange = { contribution = it }, label = { Text("Abondement employeur/mois (optionnel)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {}, readOnly = true, label = { Text("Devise") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) { Currency.entries.forEach { c -> DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") }, onClick = { selectedCurrency = c; currencyExpanded = false }) } }
            }
            Button(onClick = { onSave(savingsType, label, balance, contribution, selectedCurrency) }, enabled = label.isNotBlank() && balance.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true, modifier = Modifier.fillMaxWidth()) { Text("Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEmployeeSavingsSheet(
    savings: EmployeeSavings,
    onSave: (EmployeeSavingsType, String, String, String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var savingsType by remember { mutableStateOf(savings.type) }
    var label by remember { mutableStateOf(savings.label) }
    var balance by remember { mutableStateOf("%.2f".format(savings.currentBalanceCents / 100.0).replace(',', '.')) }
    var contribution by remember { mutableStateOf(if (savings.employerContributionCents > 0) "%.2f".format(savings.employerContributionCents / 100.0).replace(',', '.') else "") }
    var selectedCurrency by remember { mutableStateOf(savings.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Modifier l'épargne salariale", style = MaterialTheme.typography.titleLarge)
            EmployeeSavingsTypeSelector(selected = savingsType, onSelect = { savingsType = it })
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Solde actuel") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contribution, onValueChange = { contribution = it }, label = { Text("Abondement employeur/mois (optionnel)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {}, readOnly = true, label = { Text("Devise") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) { Currency.entries.forEach { c -> DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") }, onClick = { selectedCurrency = c; currencyExpanded = false }) } }
            }
            Button(onClick = { onSave(savingsType, label, balance, contribution, selectedCurrency) }, enabled = label.isNotBlank() && balance.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer les modifications") }
        }
    }
}

// ─── Graphique barres ────────────────────────────────────────────────────────

@Composable
private fun AssetsBarChart(
    realEstate: List<RealEstateAsset>,
    scpi: List<ScpiInvestment>,
    targetCurrency: Currency,
    rates: ExchangeRates
) {
    val gold = MaterialTheme.colorScheme.primary
    val sage = MaterialTheme.colorScheme.tertiary

    // Convertit chaque actif vers la devise d'affichage (summaryCurrency) : les actifs
    // peuvent être dans des devises différentes, un graphique comparatif doit les
    // ramener à une base commune pour que les longueurs de barres soient comparables.
    val entries = (
        realEstate.map { asset ->
            HorizontalBarEntry(
                label = asset.label,
                value = CurrencyConverter.convertCents(asset.currentValueCents, asset.currency, targetCurrency, rates),
                valueLabel = CurrencyConverter.convertCents(asset.currentValueCents, asset.currency, targetCurrency, rates)
                    .toCurrencyDisplay(targetCurrency),
                color = gold
            )
        } +
        scpi.map { investment ->
            HorizontalBarEntry(
                label = investment.label,
                value = CurrencyConverter.convertCents(investment.totalValueCents, investment.currency, targetCurrency, rates),
                valueLabel = CurrencyConverter.convertCents(investment.totalValueCents, investment.currency, targetCurrency, rates)
                    .toCurrencyDisplay(targetCurrency),
                color = sage
            )
        }
    ).filter { it.value > 0 }

    if (entries.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Valeur par actif", style = MaterialTheme.typography.titleMedium)
            HorizontalBarChart(entries = entries)
        }
    }
}

