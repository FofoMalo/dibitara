package com.dibitara.app.presentation.projection

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
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.model.EventProjecte
import com.dibitara.app.domain.model.SensFlux
import com.dibitara.app.presentation.common.toCurrencyDisplay
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProjectionDetailViewModel = hiltViewModel()
) {
    val projection by viewModel.projection.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projection 30 jours") },
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
            val proj = projection
            if (proj == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                ProjectionDetailContent(projection = proj)
            }
        }
    }
}

@Composable
private fun ProjectionDetailContent(projection: CashflowProjection) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM")
    val producer = remember { ChartEntryModelProducer() }
    val labels = remember(projection.pointsTimeline) {
        projection.pointsTimeline.map { it.date.format(dateFmt) }
    }

    LaunchedEffect(projection.pointsTimeline) {
        producer.setEntries(
            projection.pointsTimeline.mapIndexed { i, pt ->
                entryOf(i.toFloat(), pt.soldeCents.toFloat() / 100f)
            }
        )
    }

    // Group events by date for display
    val evenementsParDate = remember(projection.evenementsAVenir) {
        projection.evenementsAVenir.groupBy { it.date }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Line chart
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Courbe de trésorerie", style = MaterialTheme.typography.titleMedium)
                    ProvideChartStyle(m3ChartStyle()) {
                        Chart(
                            chart = lineChart(),
                            chartModelProducer = producer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(
                                valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                    labels.getOrElse(value.toInt()) { "" }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }

        // Summary row
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Aujourd'hui",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            projection.soldeActuelCents.toCurrencyDisplay(projection.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Dans 30 jours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            projection.soldeProjecte30jCents.toCurrencyDisplay(projection.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (projection.soldeProjecte30jCents < 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Warning if threshold crossed
        if (projection.jourPassageSeuilNegatif != null) {
            item {
                Text(
                    text = "Solde sous le seuil à partir du ${projection.jourPassageSeuilNegatif.format(dateFmt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Section title for events
        item {
            Text(
                "Engagements à venir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (projection.evenementsAVenir.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun engagement prévu sur 30 jours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            evenementsParDate.entries
                .sortedBy { it.key }
                .forEach { (date, evenements) ->
                    // Date separator
                    item(key = "date_${date}") {
                        Text(
                            text = date.format(dateFmt),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(evenements, key = { evt -> "${date}_${evt.label}_${evt.montantCents}" }) { evt ->
                        EvenementRow(event = evt, currency = projection.currency)
                    }
                }
        }
    }
}

@Composable
private fun EvenementRow(event: EventProjecte, currency: com.dibitara.app.domain.model.Currency) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM")
    val isEntree = event.sens == SensFlux.ENTREE
    val color = if (isEntree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (isEntree) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$sign${event.montantCents.toCurrencyDisplay(currency)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
