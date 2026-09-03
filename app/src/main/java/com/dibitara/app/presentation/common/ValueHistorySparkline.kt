package com.dibitara.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.Currency
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mini-courbe d'évolution de la valeur d'un actif dans le temps, alimentée par
 * son historique de snapshots (un point par mois, fourni déjà agrégé par
 * GetAssetValuationHistoryUseCase). Affichée sous le bloc [AcquisitionEvolutionBlock]
 * sur la carte d'un actif libre (écran Placements) - pensée d'abord pour un PEA
 * suivi manuellement, dont l'intérêt est justement de voir la valeur bouger.
 *
 * Le tracé Canvas (aire + ligne + points) et les repères min/max reprennent
 * volontairement ceux de PatrimoineEvolutionCard (écran Patrimoine) : même
 * formule de normalisation Y. Pas encore factorisé en un seul composant - l'un
 * est une carte pleine, l'autre une section interne ; à mutualiser si un 3e
 * usage apparaît.
 *
 * Ne s'affiche pas sous 2 points : un seul relevé ne dit rien d'une tendance
 * (même garde que CalculerTendanceActifUseCase).
 */
@Composable
fun ValueHistorySparkline(
    history: List<AssetValuationSnapshot>,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) return

    val moisFormatter = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)
    val values = history.map { it.valueCents.toFloat() }
    val min = values.min()
    val max = values.max()
    // Si tous les points sont identiques, on étire artificiellement la plage pour
    // que la ligne reste centrée plutôt que collée en haut.
    val range = (max - min).takeIf { it > 0f } ?: (max.takeIf { it != 0f } ?: 1f)

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.12f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Évolution", style = MaterialTheme.typography.titleSmall)
        Text(
            "${history.size} mois de données",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val w = size.width
            val h = size.height
            val xStep = if (values.size > 1) w / (values.size - 1) else w

            // Coordonnée Y normalisée (0 = bas, h = haut)
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
                drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(i * xStep, yFor(v)))
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

        // Valeurs aux deux bouts, alignées sur les libellés de mois (le premier
        // point à gauche, le dernier à droite) : on lit ainsi "de X à Y" dans le
        // même sens que la pente. Volontairement différent de PatrimoineEvolutionCard
        // (min/max) - ici la courbe peut aussi bien monter que descendre.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                values.first().toLong().toCurrencyDisplay(currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                values.last().toLong().toCurrencyDisplay(currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
