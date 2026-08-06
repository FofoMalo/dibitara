package com.dibitara.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Une entrée du graphique en barres horizontales : [label] est affiché en entier
 * (pas de troncature par nombre de caractères fixe), [value] sert au calcul
 * proportionnel de la longueur de barre, [valueLabel] est le texte déjà formaté
 * (devise) affiché à droite.
 */
data class HorizontalBarEntry(
    val label: String,
    val value: Long,
    val valueLabel: String,
    val color: Color
)

/**
 * Graphique en barres horizontales : libellé complet à gauche, valeur à droite,
 * barre proportionnelle en dessous (refonte UX/UI 2026-08).
 *
 * Remplace le bar chart Vico vertical de l'écran Placements, dont l'axe des
 * catégories tronquait les libellés longs par un `.take(8)` sans ellipse
 * (ex. "fonciè" au lieu de "foncières des praticiens"). Ici, seul un libellé
 * réellement trop long pour la largeur d'écran est abrégé, avec une vraie
 * ellipse "…" gérée par le système - pas une coupure arbitraire à N caractères.
 */
@Composable
fun HorizontalBarChart(
    entries: List<HorizontalBarEntry>,
    modifier: Modifier = Modifier
) {
    val max = entries.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1L

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        entries.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = entry.valueLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (entry.value.toFloat() / max).coerceIn(0f, 1f))
                            .height(9.dp)
                            .background(entry.color, RoundedCornerShape(5.dp))
                    )
                }
            }
        }
    }
}
