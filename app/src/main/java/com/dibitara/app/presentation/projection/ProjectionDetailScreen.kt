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
import com.dibitara.app.presentation.common.ProjectionSparkline
import com.dibitara.app.presentation.common.toCurrencyDisplay
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
    val enDanger = projection.jourPassageSeuilNegatif != null
    val courbeColor = if (enDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

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
                    // Ni axe ni graduation quotidienne : avec 30 points, Vico tronquait chaque
                    // libellé de date ("06/08" -> "0…") quel que soit le nombre affiché, car il
                    // réserve la largeur de chaque tick selon le nombre total de points, pas selon
                    // les libellés réellement visibles - même "un label sur 5" ne suffisait pas.
                    // La courbe montre une forme (déclin, plateau, rechute), pas des valeurs à lire
                    // point par point : les dates précises sont déjà dans "Engagements à venir"
                    // ci-dessous, et les deux valeurs clés dans le résumé juste en-dessous.
                    if (projection.pointsTimeline.size >= 2) {
                        ProjectionSparkline(
                            points = projection.pointsTimeline,
                            color = courbeColor,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = projection.pointsTimeline.first().date.format(dateFmt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = projection.pointsTimeline.last().date.format(dateFmt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        EvenementRow(event = evt)
                    }
                }
        }
    }
}

@Composable
private fun EvenementRow(event: EventProjecte) {
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
            text = "$sign${event.montantCents.toCurrencyDisplay(event.currency)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
