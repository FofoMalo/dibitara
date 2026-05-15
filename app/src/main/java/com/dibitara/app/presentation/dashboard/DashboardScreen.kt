package com.dibitara.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.presentation.common.toCurrencyDisplay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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

@Composable
fun DashboardScreen(
    onNavigateToDebts        : () -> Unit = {},
    onNavigateToReport       : () -> Unit = {},
    onNavigateToBudget       : () -> Unit = {},
    onNavigateToSavings      : () -> Unit = {},
    onNavigateToInvestments  : () -> Unit = {},
    onNavigateToPatrimoine   : () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = uiState) {
            is DashboardUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is DashboardUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            is DashboardUiState.Success ->
                DashboardContent(
                    overview                = state.overview,
                    spendingHistory         = state.spendingHistory,
                    onNavigateToDebts       = onNavigateToDebts,
                    onNavigateToReport      = onNavigateToReport,
                    onNavigateToBudget      = onNavigateToBudget,
                    onNavigateToSavings     = onNavigateToSavings,
                    onNavigateToInvestments = onNavigateToInvestments,
                    onNavigateToPatrimoine  = onNavigateToPatrimoine,
                    rapportMensuel          = state.rapportMensuel
                )
        }
    }
}

@Composable
private fun DashboardContent(
    overview                : PatrimonyOverview,
    spendingHistory         : List<MonthlyExpense>,
    onNavigateToDebts       : () -> Unit,
    onNavigateToReport      : () -> Unit,
    onNavigateToBudget      : () -> Unit,
    onNavigateToSavings     : () -> Unit,
    onNavigateToInvestments : () -> Unit,
    onNavigateToPatrimoine  : () -> Unit,
    rapportMensuel          : MonthlyReport? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tableau de bord", style = MaterialTheme.typography.headlineMedium)

        PatrimonyNetCard(overview = overview, onClick = onNavigateToPatrimoine)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier   = Modifier.weight(1f),
                title      = "Liquidités",
                valueCents = overview.liquiditesCents,
                currency   = overview.currency,
                color      = MaterialTheme.colorScheme.primary,
                onClick    = onNavigateToBudget
            )
            MetricCard(
                modifier   = Modifier.weight(1f),
                title      = "Épargne",
                valueCents = overview.epargneCents,
                currency   = overview.currency,
                color      = MaterialTheme.colorScheme.secondary,
                onClick    = onNavigateToSavings
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier   = Modifier.weight(1f),
                title      = "Investissements",
                valueCents = overview.investissementsCents,
                currency   = overview.currency,
                color      = MaterialTheme.colorScheme.tertiary,
                onClick    = onNavigateToInvestments
            )
            MetricCard(
                modifier   = Modifier.weight(1f),
                title      = "Revenus locatifs (année)",
                valueCents = overview.airbnbAnnualRevenueCents,
                currency   = overview.currency,
                color      = MaterialTheme.colorScheme.tertiary,
                onClick    = onNavigateToInvestments
            )
        }

        DebtsCard(
            totalCents = overview.dettesTotalCents,
            currency = overview.currency,
            onClick = onNavigateToDebts
        )

        // Rapport synthèse OU graphique 6 mois selon le réglage utilisateur
        if (rapportMensuel != null) {
            RapportSyntheseCard(rapport = rapportMensuel, onVoirDetail = onNavigateToReport)
        } else if (spendingHistory.any { it.totalCents > 0 }) {
            SpendingHistoryCard(history = spendingHistory, currency = overview.currency)
        }
    }
}

@Composable
private fun SpendingHistoryCard(history: List<MonthlyExpense>, currency: Currency) {
    // ChartEntryModelProducer gère les mises à jour asynchrones des données du graphique
    val producer = remember { ChartEntryModelProducer() }
    val labels = remember(history) { history.map { moisAbrege(it.month) } }

    LaunchedEffect(history) {
        // Conversion centimes → euros, x = index du mois dans la liste
        producer.setEntries(
            history.mapIndexed { i, expense ->
                entryOf(i.toFloat(), expense.totalCents.toFloat() / 100f)
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Dépenses — 6 derniers mois", style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun PatrimonyNetCard(overview: PatrimonyOverview, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Patrimoine brut",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Voir le détail du patrimoine",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
            Text(
                overview.patrimoineBrutCents.toCurrencyDisplay(overview.currency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))
            Text("Patrimoine net", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(
                overview.patrimoineNetCents.toCurrencyDisplay(overview.currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier   : Modifier = Modifier,
    title      : String,
    valueCents : Long,
    currency   : Currency,
    color      : androidx.compose.ui.graphics.Color,
    onClick    : (() -> Unit)? = null
) {
    // Contenu commun extrait pour éviter la duplication entre les deux surcharges de Card
    @Composable
    fun content() {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Voir le détail",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                valueCents.toCurrencyDisplay(currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }

    if (onClick != null) {
        Card(modifier = modifier, onClick = onClick) { content() }
    } else {
        Card(modifier = modifier) { content() }
    }
}

@Composable
private fun DebtsCard(totalCents: Long, currency: Currency, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (totalCents > 0) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dettes & crédits", style = MaterialTheme.typography.titleSmall)
                Text("Appuyez pour gérer", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                totalCents.toCurrencyDisplay(currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalCents > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Carte rapport synthèse compacte ─────────────────────────────────────────

@Composable
private fun RapportSyntheseCard(rapport: MonthlyReport, onVoirDetail: () -> Unit) {
    val hausse = rapport.variationDepensesCents > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // En-tête : titre + lien "Voir le détail"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Rapport — ${moisComplet(rapport.month)} ${rapport.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onVoirDetail,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Voir le détail", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bilan sur une ligne : Revenus / Dépenses / Solde
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BilanMini("Revenus",  rapport.revenusCents,  rapport.currency, MaterialTheme.colorScheme.primary)
                BilanMini("Dépenses", rapport.depensesCents, rapport.currency, MaterialTheme.colorScheme.error)
                BilanMini(
                    label      = "Solde",
                    valueCents = rapport.soldeCents,
                    currency   = rapport.currency,
                    color      = if (rapport.soldeCents >= 0) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error
                )
            }

            // Variation vs mois précédent
            if (rapport.variationDepensesCents != 0L) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (hausse) Icons.AutoMirrored.Filled.TrendingUp
                                      else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (hausse) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    val delta = if (hausse) rapport.variationDepensesCents
                                else -rapport.variationDepensesCents
                    Text(
                        text = "${if (hausse) "+" else "-"}${delta.toCurrencyDisplay(rapport.currency)} vs mois précédent",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hausse) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BilanMini(label: String, valueCents: Long, currency: Currency,
                      color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valueCents.toCurrencyDisplay(currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun moisComplet(month: Int): String = when (month) {
    1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
    9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; else -> "Décembre"
}

/** Convertit un numéro de mois (1–12) en abréviation française à 3 lettres. */
private fun moisAbrege(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"; 5 -> "Mai"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aoû"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Déc"
}
