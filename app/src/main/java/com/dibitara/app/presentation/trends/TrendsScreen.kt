package com.dibitara.app.presentation.trends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.CategoryTrend
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
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val trends by viewModel.trends.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tendances · 6 mois") },
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
            if (trends.isEmpty()) {
                Text(
                    text = "Aucune dépense sur les 6 derniers mois",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trends, key = { it.category.name }) { trend ->
                        CategoryTrendCard(trend = trend)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTrendCard(trend: CategoryTrend) {
    val producer = remember { ChartEntryModelProducer() }
    val labels = remember(trend.moisData) {
        trend.moisData.map { md ->
            Month.of(md.month)
                .getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                .replaceFirstChar { it.uppercase() }
        }
    }

    LaunchedEffect(trend.moisData) {
        producer.setEntries(
            trend.moisData.mapIndexed { i, md ->
                entryOf(i.toFloat(), md.totalCents.toFloat() / 100f)
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // En-tête : nom de la catégorie + badge de variation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trend.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                VariationBadge(variationPct = trend.variationPct)
            }

            // Mini graphique en barres Vico. Le label d'axe bas de Vico tronque tout libellé
            // de 4 caractères ou plus (ex. "Mars" -> "Ma…") quel que soit l'espace disponible -
            // même en n'affichant qu'un label sur deux, la troncature persiste (constaté à
            // l'écran) : Vico réserve la largeur de chaque tick selon le nombre total de points,
            // pas selon les libellés réellement affichés. On masque donc le label Vico et on
            // affiche les mois nous-mêmes en dessous (même stratégie que HorizontalBarChart,
            // qui avait remplacé Vico pour la même raison sur l'écran Placements).
            ProvideChartStyle(m3ChartStyle()) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = producer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { _, _ -> "" }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Total 6 mois
            Text(
                text = "Total 6 mois : ${trend.totalSixMoisCents.toCurrencyDisplay(trend.currency)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VariationBadge(variationPct: Int?) {
    val (texte, couleur) = when {
        variationPct == null -> "=" to MaterialTheme.colorScheme.onSurfaceVariant
        variationPct > 0     -> "+$variationPct%" to MaterialTheme.colorScheme.error
        variationPct < 0     -> "$variationPct%" to MaterialTheme.colorScheme.primary
        else                 -> "=" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = texte,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = couleur
    )
}
