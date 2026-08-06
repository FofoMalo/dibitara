package com.dibitara.app.presentation.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Add
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.EnveloppeStatus
import com.dibitara.app.presentation.common.DonutAvecLegende
import com.dibitara.app.presentation.common.HeroCard
import com.dibitara.app.presentation.common.chartColor
import com.dibitara.app.presentation.common.toCurrencyDisplay
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetScreen(
    // category, type, month, year sont passés en String/Int pour traverser la couche navigation sans import
    onNavigateToExpenses        : (category: String?, type: String?, month: Int, year: Int) -> Unit = { _, _, _, _ -> },
    onNavigateToTrends          : () -> Unit = {},
    onNavigateToRecommandations : () -> Unit = {},
    afficherRecommandations     : Boolean = false,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog        by remember { mutableStateOf(false) }
    var showDeleteDialog      by remember { mutableStateOf(false) }
    // null = création d'une nouvelle enveloppe ; non-null = édition d'une enveloppe existante
    var enveloppeEnEdition    by remember { mutableStateOf<EnveloppeStatus?>(null) }
    var showEnveloppeDialog   by remember { mutableStateOf(false) }
    var enveloppeASupprimer   by remember { mutableStateOf<CategoryEnvelope?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Définir le budget")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is BudgetUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is BudgetUiState.Error ->
                    Text(state.message, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center))
                is BudgetUiState.Success ->
                    BudgetContent(
                        state                       = state,
                        onPreviousMonth             = viewModel::previousMonth,
                        onNextMonth                 = viewModel::nextMonth,
                        onEditBudget                = { showEditDialog = true },
                        onDeleteBudget              = { showDeleteDialog = true },
                        onNavigateToExpenses        = onNavigateToExpenses,
                        onNavigateToTrends          = onNavigateToTrends,
                        onNavigateToRecommandations = onNavigateToRecommandations,
                        afficherRecommandations     = afficherRecommandations,
                        onAjouterEnveloppe          = {
                            enveloppeEnEdition  = null
                            showEnveloppeDialog = true
                        },
                        onEditerEnveloppe    = { statut ->
                            enveloppeEnEdition  = statut
                            showEnveloppeDialog = true
                        },
                        onSupprimerEnveloppe = { envelope ->
                            enveloppeASupprimer = envelope
                        }
                    )
            }
        }
    }

    if (showEditDialog) {
        SetBudgetDialog(
            currentBudget = (uiState as? BudgetUiState.Success)?.budget,
            revenusCents  = (uiState as? BudgetUiState.Success)?.revenusCents ?: 0L,
            onConfirm = { amount, currency ->
                viewModel.saveBudget(amount, currency)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le budget") },
            text  = { Text("Supprimer l'objectif budgétaire de ce mois ? Les transactions enregistrées ne seront pas supprimées.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.supprimerBudget()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showEnveloppeDialog) {
        val currency = (uiState as? BudgetUiState.Success)?.currency ?: Currency.EUR
        SetEnveloppeDialog(
            enveloppeExistante = enveloppeEnEdition?.envelope,
            deviseDefaut       = currency,
            onConfirm = { amountStr, category, devise ->
                viewModel.sauvegarderEnveloppe(enveloppeEnEdition?.envelope, amountStr, category, devise)
                showEnveloppeDialog = false
                enveloppeEnEdition  = null
            },
            onDismiss = {
                showEnveloppeDialog = false
                enveloppeEnEdition  = null
            }
        )
    }

    val aSupprimer = enveloppeASupprimer
    if (aSupprimer != null) {
        AlertDialog(
            onDismissRequest = { enveloppeASupprimer = null },
            title = { Text("Supprimer l'enveloppe") },
            text  = { Text("Supprimer l'enveloppe « ${aSupprimer.category.displayName} » ? Le plafond ne sera plus suivi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.supprimerEnveloppe(aSupprimer)
                        enveloppeASupprimer = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { enveloppeASupprimer = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun BudgetContent(
    state                       : BudgetUiState.Success,
    onPreviousMonth             : () -> Unit,
    onNextMonth                 : () -> Unit,
    onEditBudget                : () -> Unit,
    onDeleteBudget              : () -> Unit,
    onNavigateToExpenses        : (category: String?, type: String?, month: Int, year: Int) -> Unit,
    onNavigateToTrends          : () -> Unit = {},
    onNavigateToRecommandations : () -> Unit = {},
    afficherRecommandations     : Boolean = false,
    onAjouterEnveloppe          : () -> Unit = {},
    onEditerEnveloppe           : (EnveloppeStatus) -> Unit = {},
    onSupprimerEnveloppe        : (CategoryEnvelope) -> Unit = {}
) {
    val monthName = Month.of(state.month).getDisplayName(TextStyle.FULL, Locale.FRENCH)
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Navigateur de mois
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mois précédent")
                }
                Text("$monthName ${state.year}", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mois suivant")
                }
            }
        }

        val currency = state.currency
        val revenus  = state.transactions.filter { it.type == TransactionType.INCOME }

        // Carte fusionnée : bilan réel + objectif budget
        item {
            BilanBudgetCard(
                revenusCents  = state.revenusCents,
                depensesCents = state.depensesCents,
                soldeCents    = state.soldeCents,
                budget        = state.budget,
                currency      = currency,
                onDefinirBudget = onEditBudget,
                onDeleteBudget  = onDeleteBudget
            )
        }

        // Revenus - une seule ligne compacte cliquable, pas de liste plate
        if (revenus.isNotEmpty()) {
            item {
                RevenusCompactCard(
                    count      = revenus.size,
                    totalCents = state.revenusCents,
                    currency   = currency,
                    onClick    = { onNavigateToExpenses(null, TransactionType.INCOME.name, state.month, state.year) }
                )
            }
        }

        // Section dépenses - donut interactif + répartition par catégorie cliquable
        val depenses = state.transactions.filter { it.type == TransactionType.EXPENSE }
        if (depenses.isNotEmpty()) {
            item {
                CategoryDonutChart(
                    transactions        = depenses,
                    customSubCategories = state.customSubCategories,
                    currency            = currency,
                    onCategoryClick     = { cat ->
                        onNavigateToExpenses(cat.name, TransactionType.EXPENSE.name, state.month, state.year)
                    }
                )
            }
            item {
                Text(
                    "Dépenses par catégorie",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(categoryBreakdown(depenses)) { (category, cents) ->
                CategoryRow(
                    category    = category,
                    amountCents = cents,
                    currency    = currency,
                    onClick     = { onNavigateToExpenses(category.name, TransactionType.EXPENSE.name, state.month, state.year) }
                )
            }
        }

        // Section enveloppes budgétaires par catégorie
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Enveloppes par catégorie", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAjouterEnveloppe) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter une enveloppe")
                }
            }
        }
        if (state.enveloppeStatuts.isEmpty()) {
            item {
                Text(
                    "Aucune enveloppe définie - appuyez sur + pour en créer une.",
                    style  = MaterialTheme.typography.bodySmall,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            items(state.enveloppeStatuts) { statut ->
                EnveloppeCard(
                    statut     = statut,
                    currency   = currency,
                    onEditer   = { onEditerEnveloppe(statut) },
                    onSupprimer = { onSupprimerEnveloppe(statut.envelope) }
                )
            }
        }

        // Boutons de navigation vers les écrans secondaires
        item {
            TextButton(
                onClick = onNavigateToTrends,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir les tendances sur 6 mois →")
            }
        }
        // Le bouton recommandations n'apparaît que si l'utilisateur l'a activé dans les Paramètres
        if (afficherRecommandations) {
            item {
                TextButton(
                    onClick = onNavigateToRecommandations,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir les recommandations budgétaires →")
                }
            }
        }
    }
}

// ─── Carte fusionnée Bilan + Budget ──────────────────────────────────────────

/**
 * Carte unique regroupant le bilan réel du mois (revenus / dépenses / solde)
 * et l'objectif budget (barre de progression + restant).
 * Remplace les anciennes cartes BilanReelCard et BudgetObjectifCard.
 */
@Composable
private fun BilanBudgetCard(
    revenusCents    : Long,
    depensesCents   : Long,
    soldeCents      : Long,
    budget          : Budget?,
    currency        : Currency,
    onDefinirBudget : () -> Unit,
    onDeleteBudget  : () -> Unit
) {
    val soldePositif = soldeCents >= 0

    HeroCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ligne bilan : Revenus | Dépenses | Solde, séparés par des filets verticaux
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BilanColonne("Revenus",  revenusCents,  currency, MaterialTheme.colorScheme.primary)
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                BilanColonne("Dépenses", depensesCents, currency, MaterialTheme.colorScheme.error)
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                BilanColonne(
                    label      = "Solde",
                    valueCents = soldeCents,
                    currency   = currency,
                    color      = if (soldePositif) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error,
                    prefix     = if (soldePositif) "+" else ""
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (budget != null) {
                val progress  = if (budget.allocatedCents > 0)
                    budget.spentCents.toFloat() / budget.allocatedCents else 0f
                val isOver    = budget.isOverBudget

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Budget ${budget.allocatedCents.toCurrencyDisplay(currency)} · " +
                            "${(progress.coerceIn(0f, 1f) * 100).toInt()}% utilisé",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDeleteBudget, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Supprimer le budget",
                            tint   = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                BudgetGauge(
                    progress   = progress,
                    color      = if (isOver) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier   = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Dépensé : ${budget.spentCents.toCurrencyDisplay(currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isOver) "Dépassé de ${(-budget.remainingCents).toCurrencyDisplay(currency)}"
                        else "Restant : ${budget.remainingCents.toCurrencyDisplay(currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOver) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aucun objectif budget ce mois",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onDefinirBudget,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Définir →", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** Jauge de budget custom (11dp, coins entièrement arrondis) - remplace le LinearProgressIndicator M3 par défaut. */
@Composable
private fun BudgetGauge(progress: Float, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(11.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val fillWidth = size.width * progress.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            drawRoundRect(color = color, size = Size(fillWidth, size.height), cornerRadius = radius)
        }
    }
}

// ─── Revenus compacts ─────────────────────────────────────────────────────────

/** Ligne unique cliquable résumant tous les revenus du mois - remplace la liste plate. */
@Composable
private fun RevenusCompactCard(
    count      : Int,
    totalCents : Long,
    currency   : Currency,
    onClick    : () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Revenus", style = MaterialTheme.typography.titleSmall)
                Text(
                    "$count entrée${if (count > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "+${totalCents.toCurrencyDisplay(currency)}",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Voir les revenus",
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BilanColonne(
    label      : String,
    valueCents : Long,
    currency   : Currency,
    color      : androidx.compose.ui.graphics.Color,
    prefix     : String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$prefix${valueCents.toCurrencyDisplay(currency)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─── Objectif budget ──────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(category: Category, amountCents: Long, currency: Currency, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(amountCents.toCurrencyDisplay(currency), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetBudgetDialog(
    currentBudget: Budget?,
    revenusCents: Long,
    onConfirm: (String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember {
        mutableStateOf(currentBudget?.let { "%.2f".format(it.allocatedCents / 100.0).replace(',', '.') } ?: "")
    }
    var selectedCurrency by remember { mutableStateOf(currentBudget?.currency ?: Currency.EUR) }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentBudget != null) "Modifier le budget" else "Définir le budget mensuel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.imePadding()) {
                // Si des revenus ont été saisis, on propose une enveloppe à 80 % (règle courante : épargner 20 %)
                if (revenusCents > 0) {
                    val suggestion80 = "%.2f".format(revenusCents * 0.8 / 100.0).replace(',', '.')
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "Revenus ce mois : ${revenusCents.toCurrencyDisplay(selectedCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = { amount = suggestion80 },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Text("Suggérer 80 % - $suggestion80 ${selectedCurrency.symbol}")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "${selectedCurrency.name} (${selectedCurrency.symbol})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Devise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.name} (${currency.symbol})") },
                                onClick = { selectedCurrency = currency; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, selectedCurrency) },
                enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}


/**
 * Donut interactif des dépenses par catégorie.
 *
 * - Clic sur un segment de légende → [onCategoryClick] pour naviguer vers les transactions filtrées.
 * - Clic sur "Autre" → drill-down : le donut affiche la répartition des sous-catégories de AUTRE.
 * - Un bouton "← Retour" permet de revenir à la vue principale depuis le drill-down.
 */
@Composable
private fun CategoryDonutChart(
    transactions: List<Transaction>,
    customSubCategories: List<CustomSubCategory>,
    currency: Currency,
    onCategoryClick: (Category) -> Unit
) {
    // null = vue principale ; Category.AUTRE = drill-down sous-catégories
    var drillDown by remember { mutableStateOf(false) }

    val total = transactions.sumOf { it.amountCents }.toFloat()
    val groupesPrincipaux = transactions
        .groupBy { it.category }
        .map { (cat, txs) -> cat to txs.sumOf { it.amountCents } }
        .sortedByDescending { it.second }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête : titre + bouton retour si drill-down actif
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (drillDown) {
                    IconButton(onClick = { drillDown = false }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour aux catégories")
                    }
                    Text("Détail - Autre", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Répartition des dépenses", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(48.dp)) // équilibre la mise en page quand pas de bouton retour
                }
            }

            if (drillDown) {
                // ── Vue drill-down : répartition des sous-catégories de AUTRE ──
                val autreTxs = transactions.filter { it.category == Category.AUTRE }
                val subGroupes = autreTxs
                    .groupBy { tx ->
                        // Résolution du libellé : CustomSubCategory > SubCategory enum > "Non classé"
                        tx.customSubCategoryId
                            ?.let { id -> customSubCategories.find { it.id == id }?.name }
                            ?: tx.subCategory?.displayName
                            ?: "Non classé"
                    }
                    .map { (label, txs) -> label to txs.sumOf { it.amountCents } }
                    .sortedByDescending { it.second }
                val totalAutre = autreTxs.sumOf { it.amountCents }.toFloat()

                DonutAvecLegende(
                    groupes  = subGroupes,
                    total    = totalAutre,
                    currency = currency,
                    // Depuis le drill-down, cliquer navigue vers Autres (filtre catégorie = AUTRE)
                    onItemClick = { onCategoryClick(Category.AUTRE) }
                )
            } else {
                // ── Vue principale : répartition par catégorie ──
                // Couleurs stables par catégorie (CategoryVisuals) plutôt que positionnelles :
                // une catégorie garde la même couleur d'un mois à l'autre.
                DonutAvecLegende(
                    groupes  = groupesPrincipaux.map { (cat, cents) -> cat.displayName to cents },
                    total    = total,
                    currency = currency,
                    couleurs = groupesPrincipaux.map { (cat, _) -> cat.chartColor() },
                    onItemClick = { label ->
                        val cat = groupesPrincipaux.firstOrNull { it.first.displayName == label }?.first
                        if (cat == Category.AUTRE) {
                            drillDown = true  // ouvre le drill-down au lieu de naviguer
                        } else if (cat != null) {
                            onCategoryClick(cat)
                        }
                    },
                    // Indicateur visuel sur AUTRE : "▶" pour signaler le drill-down disponible
                    trailingLabel = { label ->
                        if (label == Category.AUTRE.displayName) " ▶" else ""
                    }
                )
            }
        }
    }
}


// ─── Enveloppe budgétaire ─────────────────────────────────────────────────────

/**
 * Carte d'une enveloppe : nom catégorie, barre de progression colorée,
 * montant dépensé / plafond, boutons éditer / supprimer.
 *
 * Couleur de la barre :
 *  - Vert  (<80 %)  → Primary
 *  - Orange (80-99 %) → Tertiary (ou Custom color)
 *  - Rouge  (≥100 %) → Error
 */
@Composable
private fun EnveloppeCard(
    statut     : EnveloppeStatus,
    currency   : Currency,
    onEditer   : () -> Unit,
    onSupprimer: () -> Unit
) {
    val couleurBarre = when {
        statut.isDepasse -> MaterialTheme.colorScheme.error
        statut.isAlerte  -> Color(0xFFE68A00)  // orange - pas dans le colorScheme M3 par défaut
        else             -> MaterialTheme.colorScheme.primary
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    statut.envelope.category.displayName,
                    style      = MaterialTheme.typography.titleSmall,
                    modifier   = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                            modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary) },
                            onClick = { showMenu = false; onEditer() }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onSupprimer() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { statut.taux.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color    = couleurBarre
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Dépensé : ${statut.depenseCents.toCurrencyDisplay(currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (statut.isDepasse)
                        "Dépassé de ${(statut.depenseCents - statut.envelope.plafondCents).toCurrencyDisplay(currency)}"
                    else
                        "Plafond : ${statut.envelope.plafondCents.toCurrencyDisplay(currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (statut.isDepasse) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog de création / édition d'une enveloppe.
 * Si [enveloppeExistante] est non-null, le champ catégorie est pré-rempli et verrouillé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetEnveloppeDialog(
    enveloppeExistante: CategoryEnvelope?,
    deviseDefaut      : Currency,
    onConfirm         : (amountStr: String, category: Category, currency: Currency) -> Unit,
    onDismiss         : () -> Unit
) {
    // Catégories disponibles - on exclut AUTRE car elle regroupe les "divers" non classés
    val categoriesDisponibles = Category.entries.filter { it != Category.AUTRE }

    var amount           by remember { mutableStateOf(enveloppeExistante?.let { "%.2f".format(it.plafondCents / 100.0).replace(',', '.') } ?: "") }
    var selectedCategory by remember { mutableStateOf(enveloppeExistante?.category ?: categoriesDisponibles.first()) }
    var selectedCurrency by remember { mutableStateOf(enveloppeExistante?.currency ?: deviseDefaut) }
    var expandedCat      by remember { mutableStateOf(false) }
    var expandedDev      by remember { mutableStateOf(false) }
    val focusManager     = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (enveloppeExistante != null) "Modifier l'enveloppe" else "Nouvelle enveloppe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.imePadding()) {
                // Sélecteur de catégorie (verrouillé en édition)
                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = {
                    if (enveloppeExistante == null) expandedCat = it
                }) {
                    OutlinedTextField(
                        value         = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Catégorie") },
                        trailingIcon  = {
                            if (enveloppeExistante == null) ExposedDropdownMenuDefaults.TrailingIcon(expandedCat)
                        },
                        modifier      = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    if (enveloppeExistante == null) {
                        ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            categoriesDisponibles.forEach { cat ->
                                DropdownMenuItem(
                                    text    = { Text(cat.displayName) },
                                    onClick = { selectedCategory = cat; expandedCat = false }
                                )
                            }
                        }
                    }
                }

                // Montant
                OutlinedTextField(
                    value          = amount,
                    onValueChange  = { amount = it },
                    label          = { Text("Plafond mensuel") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth()
                )

                // Devise
                ExposedDropdownMenuBox(expanded = expandedDev, onExpandedChange = { expandedDev = it }) {
                    OutlinedTextField(
                        value         = "${selectedCurrency.name} (${selectedCurrency.symbol})",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Devise") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDev) },
                        modifier      = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedDev, onDismissRequest = { expandedDev = false }) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text    = { Text("${currency.name} (${currency.symbol})") },
                                onClick = { selectedCurrency = currency; expandedDev = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, selectedCategory, selectedCurrency) },
                enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun categoryBreakdown(transactions: List<Transaction>): List<Pair<Category, Long>> =
    transactions
        .groupBy { it.category }
        .map { (cat, txs) -> cat to txs.sumOf { it.amountCents } }
        .sortedByDescending { it.second }


