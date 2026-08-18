package com.dibitara.app.presentation.patrimoine

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.presentation.common.DonutAvecLegende
import com.dibitara.app.presentation.common.HeroCard
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrimoineDetailScreen(
    onNavigateBack        : () -> Unit,
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
                        history               = state.history,
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
    history               : List<PatrimoineSnapshot>,
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
        HeroCard {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    "Patrimoine brut",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    overview.patrimoineBrutCents.toCurrencyDisplay(overview.currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Répartition visuelle des actifs ─────────────────────────────────
        PatrimoineDonutCard(overview)

        // ── Évolution mensuelle du patrimoine net ────────────────────────────
        PatrimoineEvolutionCard(history = history, currency = overview.currency)

        // ── Décomposition des actifs ─────────────────────────────────────────
        // Le budget restant (liquidités) n'y figure pas : c'est un flux mensuel, pas un actif
        // (voir PatrimonyOverview.patrimoineBrutCents) - visible séparément sur le Dashboard.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
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
        HeroCard {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Patrimoine net",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    overview.patrimoineNetCents.toCurrencyDisplay(overview.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netPositif) MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "= Patrimoine brut − Dettes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Graphique d'évolution mensuelle du patrimoine net ───────────────────────

/**
 * Carte affichant un sparkline du patrimoine net sur les derniers mois.
 * Un snapshot est enregistré une fois par jour à l'ouverture de l'écran,
 * puis agrégé en un point mensuel (dernier snapshot du mois).
 * La carte reste masquée tant qu'il n'y a pas au moins 2 mois de données.
 */
@Composable
private fun PatrimoineEvolutionCard(
    history : List<PatrimoineSnapshot>,
    currency: Currency
) {
    if (history.size < 2) return

    val moisFormatter = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)
    val values = history.map { it.patrimoineNetCents.toFloat() }
    val min = values.min()
    val max = values.max()
    // Si tous les points sont identiques, on étire artificiellement la plage pour
    // que la ligne reste au centre plutôt qu'en haut
    val range = (max - min).takeIf { it > 0f } ?: (max.takeIf { it != 0f } ?: 1f)

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.12f)
    val dotColor  = lineColor

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Évolution du patrimoine net", style = MaterialTheme.typography.titleMedium)
            Text(
                "${history.size} mois de données",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val w = size.width
                val h = size.height
                val xStep = if (values.size > 1) w / (values.size - 1) else w

                // Calcule la coordonnée Y normalisée (0 = bas, h = haut)
                fun yFor(v: Float) = h - ((v - min) / range * h).coerceIn(0f, h)

                // Surface colorée sous la courbe
                val fillPath = Path().apply {
                    moveTo(0f, h)
                    values.forEachIndexed { i, v -> lineTo(i * xStep, yFor(v)) }
                    lineTo((values.size - 1) * xStep, h)
                    close()
                }
                drawPath(fillPath, fillColor)

                // Ligne de la courbe
                val linePath = Path().apply {
                    values.forEachIndexed { i, v ->
                        val x = i * xStep
                        val y = yFor(v)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx()))

                // Points de données
                values.forEachIndexed { i, v ->
                    drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(i * xStep, yFor(v)))
                }
            }

            // Étiquettes de l'axe X : mois abrégés
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                history.forEach { snapshot ->
                    Text(
                        snapshot.snapshotDate.format(moisFormatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Repères de valeur : min et max visibles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    min.toLong().toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    max.toLong().toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Donut de répartition des actifs ─────────────────────────────────────────

/**
 * Camembert (donut) montrant la décomposition du patrimoine brut en 2 segments :
 * épargne (secondary), investissements (tertiary). Le budget restant (liquidités) n'y figure
 * pas : c'est un flux mensuel, pas un actif (voir PatrimonyOverview.patrimoineBrutCents).
 * Les segments à 0 sont ignorés. La carte n'est pas affichée si le brut est nul.
 */
@Composable
private fun PatrimoineDonutCard(overview: PatrimonyOverview) {
    val brut = overview.patrimoineBrutCents
    if (brut <= 0L) return

    val groupes = buildList {
        if (overview.epargneCents       > 0L) add("Épargne"         to overview.epargneCents)
        if (overview.investissementsCents > 0L) add("Investissements" to overview.investissementsCents)
    }
    if (groupes.isEmpty()) return

    // Couleurs M3 sémantiques : cohérence avec la décomposition textuelle ci-dessous
    val couleurs = listOf(
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
