package com.dibitara.app.presentation.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
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
                        state = state,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onEditBudget = { showDialog = true }
                    )
            }
        }
    }

    if (showDialog) {
        SetBudgetDialog(
            currentBudget = (uiState as? BudgetUiState.Success)?.budget,
            onConfirm = { amount, currency ->
                viewModel.saveBudget(amount, currency)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun BudgetContent(
    state: BudgetUiState.Success,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditBudget: () -> Unit
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
                BudgetObjectifCard(budget = state.budget)
            } else {
                NoBudgetCard(onSetBudget = onEditBudget)
            }
        }

        // Section revenus — visible s'il y a des transactions INCOME
        val revenus = state.transactions.filter { it.type == TransactionType.INCOME }
        if (revenus.isNotEmpty()) {
            item {
                Text(
                    "Revenus du mois",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(revenus.sortedByDescending { it.amountCents }) { tx ->
                RevenuRow(transaction = tx, currency = currency)
            }
        }

        // Section dépenses — donut + répartition par catégorie
        val depenses = state.transactions.filter { it.type == TransactionType.EXPENSE }
        if (depenses.isNotEmpty()) {
            item {
                CategoryDonutChart(transactions = depenses, currency = currency)
            }
            item {
                Text(
                    "Dépenses par catégorie",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(categoryBreakdown(depenses)) { (category, cents) ->
                CategoryRow(category = category, amountCents = cents, currency = currency)
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

/** Carte d'objectif budgétaire — défini manuellement par l'utilisateur. */
@Composable
private fun BudgetObjectifCard(budget: Budget) {
    val progress = if (budget.allocatedCents > 0)
        budget.spentCents.toFloat() / budget.allocatedCents else 0f
    val isOver = budget.isOverBudget

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Objectif budget",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enveloppe allouée", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(budget.allocatedCents.toCurrencyDisplay(budget.currency),
                    style = MaterialTheme.typography.bodyLarge)
            }
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
private fun RevenuRow(transaction: Transaction, currency: Currency) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
private fun CategoryRow(category: Category, amountCents: Long, currency: Currency) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(category.displayName, style = MaterialTheme.typography.bodyMedium)
        Text(amountCents.toCurrencyDisplay(currency), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetBudgetDialog(
    currentBudget: Budget?,
    onConfirm: (String, Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember {
        mutableStateOf(currentBudget?.let { "%.2f".format(it.allocatedCents / 100.0) } ?: "")
    }
    var selectedCurrency by remember { mutableStateOf(currentBudget?.currency ?: Currency.EUR) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentBudget != null) "Modifier le budget" else "Définir le budget mensuel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun CategoryDonutChart(transactions: List<Transaction>, currency: Currency) {
    val total = transactions.sumOf { it.amountCents }.toFloat()
    val groupes = transactions
        .groupBy { it.category }
        .map { (cat, txs) -> cat to txs.sumOf { it.amountCents } }
        .sortedByDescending { it.second }

    // Une couleur distincte par catégorie
    val couleurs = listOf(
        Color(0xFF1DB954), Color(0xFF2196F3), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFFF5722), Color(0xFF607D8B)
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Répartition des dépenses", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cercle donut dessiné avec Canvas
                Canvas(modifier = Modifier.size(110.dp)) {
                    var angleDepart = -90f
                    groupes.forEachIndexed { i, (_, cents) ->
                        val balayage = (cents.toFloat() / total) * 360f
                        drawArc(
                            color = couleurs[i % couleurs.size],
                            startAngle = angleDepart,
                            sweepAngle = balayage,
                            useCenter = false,
                            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                        )
                        angleDepart += balayage
                    }
                }
                // Légende associée
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groupes.forEachIndexed { i, (categorie, cents) ->
                        val pct = (cents.toFloat() / total * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .then(Modifier) // placeholder pour la couleur
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = couleurs[i % couleurs.size])
                                }
                            }
                            Text(
                                "${categorie.displayName} $pct%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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

