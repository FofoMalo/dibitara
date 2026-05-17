package com.dibitara.app.presentation.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.presentation.common.toCurrencyDisplay

@Composable
fun MonthlyReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ─── En-tête avec retour ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text(
                text = when (val s = uiState) {
                    is MonthlyReportUiState.Success ->
                        "Rapport — ${nomMois(s.report.month)} ${s.report.year}"
                    else -> "Rapport mensuel"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        when (val state = uiState) {
            is MonthlyReportUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is MonthlyReportUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }

            is MonthlyReportUiState.Success ->
                ReportContent(report = state.report)
        }
    }
}

// ─── Contenu principal ────────────────────────────────────────────────────────

@Composable
private fun ReportContent(report: MonthlyReport) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BilanCard(report)
        if (report.budget != null) BudgetCard(report)
        if (report.topCategories.isNotEmpty()) TopCategoriesCard(report)
        VariationCard(report)
        Spacer(Modifier.height(8.dp))
    }
}

// ─── Carte Bilan ─────────────────────────────────────────────────────────────

@Composable
private fun BilanCard(report: MonthlyReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bilan du mois", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BilanItem(
                    label = "Revenus",
                    valueCents = report.revenusCents,
                    report = report,
                    color = MaterialTheme.colorScheme.primary
                )
                BilanItem(
                    label = "Dépenses",
                    valueCents = report.depensesCents,
                    report = report,
                    color = MaterialTheme.colorScheme.error
                )
                BilanItem(
                    label = "Solde",
                    valueCents = report.soldeCents,
                    report = report,
                    color = if (report.soldeCents >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BilanItem(
    label: String,
    valueCents: Long,
    report: MonthlyReport,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            valueCents.toCurrencyDisplay(report.currency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─── Carte Budget ─────────────────────────────────────────────────────────────

@Composable
private fun BudgetCard(report: MonthlyReport) {
    val budget = report.budget ?: return
    val progress = if (budget.allocatedCents > 0)
        (budget.spentCents.toFloat() / budget.allocatedCents).coerceIn(0f, 1f)
    else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (budget.isOverBudget) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Budget mensuel", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${budget.spentCents.toCurrencyDisplay(report.currency)} dépensés",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "sur ${budget.allocatedCents.toCurrencyDisplay(report.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (budget.isOverBudget) {
                Text(
                    "Budget dépassé de ${(-budget.remainingCents).toCurrencyDisplay(report.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    "Reste ${budget.remainingCents.toCurrencyDisplay(report.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Carte Top catégories ─────────────────────────────────────────────────────

@Composable
private fun TopCategoriesCard(report: MonthlyReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Top dépenses par catégorie", style = MaterialTheme.typography.titleMedium)
            report.topCategories.forEach { catExp ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            catExp.displayLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${catExp.totalCents.toCurrencyDisplay(report.currency)} " +
                            "(${catExp.pourcentage.toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { catExp.pourcentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─── Carte Variation vs M-1 ───────────────────────────────────────────────────

@Composable
private fun VariationCard(report: MonthlyReport) {
    val hausse = report.variationDepensesCents > 0
    val neutre = report.variationDepensesCents == 0L

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("vs mois précédent", style = MaterialTheme.typography.titleSmall)
                Text("Variation des dépenses", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!neutre) {
                    Icon(
                        imageVector = if (hausse) Icons.AutoMirrored.Filled.TrendingUp
                                      else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (hausse) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = when {
                        neutre -> "Stable"
                        hausse -> "+${report.variationDepensesCents.toCurrencyDisplay(report.currency)}"
                        else   -> "-${(-report.variationDepensesCents).toCurrencyDisplay(report.currency)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        neutre -> MaterialTheme.colorScheme.onSurface
                        hausse -> MaterialTheme.colorScheme.error
                        else   -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

// ─── Utilitaires ──────────────────────────────────────────────────────────────

private fun nomMois(month: Int): String = when (month) {
    1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
    9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; else -> "Décembre"
}


