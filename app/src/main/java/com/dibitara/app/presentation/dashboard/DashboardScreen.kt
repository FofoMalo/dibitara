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
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.presentation.common.toCurrencyDisplay

@Composable
fun DashboardScreen(
    onNavigateToDebts: () -> Unit = {},
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
                DashboardContent(overview = state.overview, onNavigateToDebts = onNavigateToDebts)
        }
    }
}

@Composable
private fun DashboardContent(overview: PatrimonyOverview, onNavigateToDebts: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tableau de bord", style = MaterialTheme.typography.headlineMedium)

        // Carte patrimoine net — valeur phare
        PatrimonyNetCard(overview = overview)

        // 4 métriques en 2 colonnes
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Liquidités",
                valueCents = overview.liquiditesCents,
                currency = overview.currency,
                color = MaterialTheme.colorScheme.primary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Épargne",
                valueCents = overview.epargneCents,
                currency = overview.currency,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Investissements",
                valueCents = overview.investissementsCents,
                currency = overview.currency,
                color = MaterialTheme.colorScheme.tertiary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Revenus Airbnb (année)",
                valueCents = overview.airbnbAnnualRevenueCents,
                currency = overview.currency,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Carte dettes
        DebtsCard(
            totalCents = overview.dettesTotalCents,
            currency = overview.currency,
            onClick = onNavigateToDebts
        )
    }
}

@Composable
private fun PatrimonyNetCard(overview: PatrimonyOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Patrimoine brut", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
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
    modifier: Modifier = Modifier,
    title: String,
    valueCents: Long,
    currency: Currency,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valueCents.toCurrencyDisplay(currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
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

