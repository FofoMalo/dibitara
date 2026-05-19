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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetScreen(
    // category et type sont les noms d'enum (String) pour traverser la couche navigation sans import
    onNavigateToExpenses: (category: String?, type: String?) -> Unit = { _, _ -> },
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                        state                = state,
                        onPreviousMonth      = viewModel::previousMonth,
                        onNextMonth          = viewModel::nextMonth,
                        onEditBudget         = { showEditDialog = true },
                        onDeleteBudget       = { showDeleteDialog = true },
                        onNavigateToExpenses = onNavigateToExpenses
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
}

@Composable
private fun BudgetContent(
    state: BudgetUiState.Success,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditBudget: () -> Unit,
    onDeleteBudget: () -> Unit,
    onNavigateToExpenses: (category: String?, type: String?) -> Unit
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

        val currency = state.budget?.currency ?: Currency.EUR

        // Bilan réel — toujours affiché (même si tout est à 0)
        item {
            BilanReelCard(
                revenusCents  = state.revenusCents,
                depensesCents = state.depensesCents,
                soldeCents    = state.soldeCents,
                currency      = currency
            )
        }

        // Objectif budget (optionnel — défini par l'utilisateur)
        item {
            if (state.budget != null) {
                BudgetObjectifCard(
                    budget        = state.budget,
                    revenusCents  = state.revenusCents,
                    onDelete      = onDeleteBudget
                )
            } else {
                NoBudgetCard(onSetBudget = onEditBudget)
            }
        }

        // Section revenus — cliquable → Expenses filtré par INCOME
        val revenus = state.transactions.filter { it.type == TransactionType.INCOME }
        if (revenus.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Revenus du mois", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onNavigateToExpenses(null, TransactionType.INCOME.name) }) {
                        Text("Voir tout")
                    }
                }
            }
            items(revenus.sortedByDescending { it.amountCents }) { tx ->
                RevenuRow(
                    transaction = tx,
                    currency = currency,
                    onClick = { onNavigateToExpenses(null, TransactionType.INCOME.name) }
                )
            }
        }

        // Section dépenses — donut interactif + répartition par catégorie cliquable
        val depenses = state.transactions.filter { it.type == TransactionType.EXPENSE }
        if (depenses.isNotEmpty()) {
            item {
                CategoryDonutChart(
                    transactions        = depenses,
                    customSubCategories = state.customSubCategories,
                    currency            = currency,
                    onCategoryClick     = { cat ->
                        onNavigateToExpenses(cat.name, TransactionType.EXPENSE.name)
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
                    onClick     = { onNavigateToExpenses(category.name, TransactionType.EXPENSE.name) }
                )
            }
        }
    }
}

// ─── Bilan réel ───────────────────────────────────────────────────────────────

/**
 * Carte toujours visible montrant le bilan réel du mois :
 * revenus (transactions INCOME) − dépenses (transactions EXPENSE) = solde.
 * C'est la réalité financière, indépendante du budget objectif.
 */
@Composable
private fun BilanReelCard(
    revenusCents  : Long,
    depensesCents : Long,
    soldeCents    : Long,
    currency      : Currency
) {
    val soldePositif = soldeCents >= 0
    val couleurSolde = if (soldePositif) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Bilan du mois",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BilanColonne("Revenus",  revenusCents,  currency, MaterialTheme.colorScheme.primary)
                BilanColonne("Dépenses", depensesCents, currency, MaterialTheme.colorScheme.error)
                BilanColonne(
                    label      = "Solde",
                    valueCents = soldeCents,
                    currency   = currency,
                    color      = couleurSolde,
                    prefix     = if (soldePositif) "+" else ""
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
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
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

/** Ligne de la chaîne budgétaire : libellé à gauche, montant coloré à droite. */
@Composable
private fun BudgetLigne(
    label: String,
    valueCents: Long,
    currency: Currency,
    color: Color,
    gras: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valueCents.toCurrencyDisplay(currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (gras) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

/**
 * Carte d'objectif budgétaire.
 * Si des revenus ont été saisis, affiche la chaîne complète :
 *   Revenus − Budget alloué = Épargne prévue
 * puis la barre dépensé / alloué.
 */
@Composable
private fun BudgetObjectifCard(budget: Budget, revenusCents: Long, onDelete: () -> Unit) {
    val progress = if (budget.allocatedCents > 0)
        budget.spentCents.toFloat() / budget.allocatedCents else 0f
    val isOver = budget.isOverBudget

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Objectif budget",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer le budget",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (revenusCents > 0) {
                // Chaîne : Revenus → Budget alloué → Épargne prévue
                val epargnePrevueCents = revenusCents - budget.allocatedCents
                val epargnePositive = epargnePrevueCents >= 0
                BudgetLigne("Revenus", revenusCents, budget.currency,
                    MaterialTheme.colorScheme.primary)
                BudgetLigne("− Budget alloué", budget.allocatedCents, budget.currency,
                    MaterialTheme.colorScheme.onSurface)
                HorizontalDivider()
                BudgetLigne(
                    label      = "= Épargne prévue",
                    valueCents = epargnePrevueCents,
                    currency   = budget.currency,
                    color      = if (epargnePositive) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error,
                    gras       = true
                )
            } else {
                // Pas encore de revenus saisis — affichage simple de l'enveloppe
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enveloppe allouée", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(budget.allocatedCents.toCurrencyDisplay(budget.currency),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Barre de progression : combien du budget alloué a été dépensé
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Dépensé", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(budget.spentCents.toCurrencyDisplay(budget.currency),
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Restant", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(budget.remainingCents.toCurrencyDisplay(budget.currency),
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            if (isOver) {
                Text(
                    "⚠ Budget dépassé de ${(-budget.remainingCents).toCurrencyDisplay(budget.currency)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─── Ligne revenu individuelle ────────────────────────────────────────────────

@Composable
private fun RevenuRow(transaction: Transaction, currency: Currency, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transaction.note.ifBlank { "Revenu" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "+${transaction.amountCents.toCurrencyDisplay(currency)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoBudgetCard(onSetBudget: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aucun budget défini pour ce mois", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onSetBudget) { Text("Définir un budget") }
        }
    }
}

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
                                Text("Suggérer 80 % — $suggestion80 ${selectedCurrency.symbol}")
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

// Palette de couleurs partagée par les deux vues du donut (principale et drill-down)
private val DONUT_COULEURS = listOf(
    Color(0xFF1DB954), Color(0xFF2196F3), Color(0xFFFF9800),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
    Color(0xFF4CAF50), Color(0xFFFF5722), Color(0xFF607D8B)
)

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
                    Text("Détail — Autre", style = MaterialTheme.typography.titleMedium)
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
                DonutAvecLegende(
                    groupes  = groupesPrincipaux.map { (cat, cents) -> cat.displayName to cents },
                    total    = total,
                    currency = currency,
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

/**
 * Composant réutilisable : cercle donut Canvas + légende cliquable.
 *
 * [groupes] : liste de (libellé, montant en centimes).
 * [onItemClick] : appelé avec le libellé du segment cliqué.
 * [trailingLabel] : suffixe optionnel ajouté au libellé (ex. "▶" pour le drill-down).
 */
@Composable
private fun DonutAvecLegende(
    groupes: List<Pair<String, Long>>,
    total: Float,
    currency: Currency,
    onItemClick: (String) -> Unit,
    trailingLabel: (String) -> String = { "" }
) {
    if (total == 0f || groupes.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cercle donut dessiné avec Canvas (purement visuel, les clics sont sur la légende)
        Canvas(modifier = Modifier.size(110.dp)) {
            var angleDepart = -90f
            groupes.forEachIndexed { i, (_, cents) ->
                val balayage = (cents.toFloat() / total) * 360f
                drawArc(
                    color      = DONUT_COULEURS[i % DONUT_COULEURS.size],
                    startAngle = angleDepart,
                    sweepAngle = balayage,
                    useCenter  = false,
                    style      = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                )
                angleDepart += balayage
            }
        }

        // Légende : chaque ligne est cliquable
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            groupes.forEachIndexed { i, (label, cents) ->
                val pct = (cents.toFloat() / total * 100).toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onItemClick(label) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Point de couleur
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = DONUT_COULEURS[i % DONUT_COULEURS.size])
                    }
                    Text(
                        text  = "$label$pct% · ${cents.toCurrencyDisplay(currency)}${trailingLabel(label)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun categoryBreakdown(transactions: List<Transaction>): List<Pair<Category, Long>> =
    transactions
        .groupBy { it.category }
        .map { (cat, txs) -> cat to txs.sumOf { it.amountCents } }
        .sortedByDescending { it.second }

private fun Long.toCurrencyDisplay(currency: Currency): String =
    "%.2f %s".format(this / 100.0, currency.symbol)

