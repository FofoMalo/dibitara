package com.dibitara.app.presentation.patrimoine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.presentation.common.DonutAvecLegende
import com.dibitara.app.presentation.common.toCurrencyDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrimoineDetailScreen(
    onNavigateBack        : () -> Unit,
    onNavigateToBudget    : () -> Unit,
    onNavigateToSavings   : () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToDebts     : () -> Unit,
    viewModel: PatrimoineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail du patrimoine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PatrimoineDetailUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is PatrimoineDetailUiState.Error ->
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )

                is PatrimoineDetailUiState.Success ->
                    PatrimoineDetailContent(
                        overview              = state.overview,
                        onNavigateToBudget    = onNavigateToBudget,
                        onNavigateToSavings   = onNavigateToSavings,
                        onNavigateToInvestments = onNavigateToInvestments,
                        onNavigateToDebts     = onNavigateToDebts
                    )
            }
        }
    }
}

@Composable
private fun PatrimoineDetailContent(
    overview              : PatrimonyOverview,
    onNavigateToBudget    : () -> Unit,
    onNavigateToSavings   : () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToDebts     : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Patrimoine brut ──────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    "Patrimoine brut",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    overview.patrimoineBrutCents.toCurrencyDisplay(overview.currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ── Répartition visuelle des actifs ─────────────────────────────────
        PatrimoineDonutCard(overview)

        // ── Décomposition des actifs ─────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                LigneActif(
                    label      = "Liquidités (budget du mois)",
                    valueCents = overview.liquiditesCents,
                    currency   = overview.currency,
                    color      = MaterialTheme.colorScheme.primary,
                    onClick    = onNavigateToBudget
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LigneActif(
                    label      = "Épargne",
                    valueCents = overview.epargneCents,
                    currency   = overview.currency,
                    color      = MaterialTheme.colorScheme.secondary,
                    onClick    = onNavigateToSavings
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LigneActif(
                    label      = "Investissements (immobilier + SCPI)",
                    valueCents = overview.investissementsCents,
                    currency   = overview.currency,
                    color      = MaterialTheme.colorScheme.tertiary,
                    onClick    = onNavigateToInvestments
                )
            }
        }

        // ── Dettes ───────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (overview.dettesTotalCents > 0)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                LigneActif(
                    label      = "Dettes & crédits",
                    valueCents = overview.dettesTotalCents,
                    currency   = overview.currency,
                    color      = if (overview.dettesTotalCents > 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface,
                    prefix     = if (overview.dettesTotalCents > 0) "−" else "",
                    onClick    = onNavigateToDebts
                )
            }
        }

        // ── Patrimoine net ───────────────────────────────────────────────────
        val netPositif = overview.patrimoineNetCents >= 0
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (netPositif)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Patrimoine net",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    overview.patrimoineNetCents.toCurrencyDisplay(overview.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netPositif) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.error
                )
                // Barre de santé : part du brut non engagée dans des dettes
                if (overview.patrimoineBrutCents > 0) {
                    val ratio = (overview.patrimoineNetCents.toFloat() / overview.patrimoineBrutCents.toFloat())
                        .coerceIn(0f, 1f)
                    val barreColor = if (netPositif)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color      = barreColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(ratio * 100).toInt()}% du brut non endetté",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
                Text(
                    "= Patrimoine brut − Dettes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Donut de répartition des actifs ─────────────────────────────────────────

/**
 * Camembert (donut) montrant la décomposition du patrimoine brut en 3 segments :
 * liquidités (primary), épargne (secondary), investissements (tertiary).
 * Les segments à 0 sont ignorés. La carte n'est pas affichée si le brut est nul.
 */
@Composable
private fun PatrimoineDonutCard(overview: PatrimonyOverview) {
    val brut = overview.patrimoineBrutCents
    if (brut <= 0L) return

    val groupes = buildList {
        if (overview.liquiditesCents    > 0L) add("Liquidités"      to overview.liquiditesCents)
        if (overview.epargneCents       > 0L) add("Épargne"         to overview.epargneCents)
        if (overview.investissementsCents > 0L) add("Investissements" to overview.investissementsCents)
    }
    if (groupes.isEmpty()) return

    // Couleurs M3 sémantiques : cohérence avec la décomposition textuelle ci-dessous
    val couleurs = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Répartition des actifs", style = MaterialTheme.typography.titleMedium)
            DonutAvecLegende(
                groupes  = groupes,
                total    = brut.toFloat(),
                currency = overview.currency,
                couleurs = couleurs
            )
        }
    }
}

// ─── Ligne d'actif cliquable ──────────────────────────────────────────────────

@Composable
private fun LigneActif(
    label      : String,
    valueCents : Long,
    currency   : Currency,
    color      : androidx.compose.ui.graphics.Color,
    onClick    : () -> Unit,
    prefix     : String = ""
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "$prefix${valueCents.toCurrencyDisplay(currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Voir le détail",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
