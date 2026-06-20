package com.dibitara.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dibitara.app.domain.model.Currency

// Palette de couleurs par défaut (catégories de dépenses)
val DONUT_COULEURS: List<Color> = listOf(
    Color(0xFF1DB954), Color(0xFF2196F3), Color(0xFFFF9800),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
    Color(0xFF4CAF50), Color(0xFFFF5722), Color(0xFF607D8B)
)

/**
 * Cercle donut Canvas + légende cliquable, réutilisable dans BudgetScreen
 * et PatrimoineDetailScreen.
 *
 * [groupes]      : liste (libellé, montant en centimes).
 * [couleurs]     : palette - par défaut [DONUT_COULEURS], surchargeable avec les
 *                  couleurs M3 pour un affichage cohérent avec le thème.
 * [onItemClick]  : appelé avec le libellé du segment cliqué.
 * [trailingLabel]: suffixe optionnel ajouté au libellé (ex. "▶" pour drill-down).
 */
@Composable
fun DonutAvecLegende(
    groupes      : List<Pair<String, Long>>,
    total        : Float,
    currency     : Currency,
    onItemClick  : (String) -> Unit = {},
    trailingLabel: (String) -> String = { "" },
    couleurs     : List<Color> = DONUT_COULEURS
) {
    if (total == 0f || groupes.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cercle donut dessiné avec Canvas (purement visuel, les clics sont sur la légende)
        Canvas(modifier = Modifier.size(110.dp)) {
            var angleDepart = -90f
            groupes.forEachIndexed { i, (_, cents) ->
                val balayage = (cents.toFloat() / total) * 360f
                drawArc(
                    color      = couleurs[i % couleurs.size],
                    startAngle = angleDepart,
                    sweepAngle = balayage,
                    useCenter  = false,
                    style      = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                )
                angleDepart += balayage
            }
        }

        // Légende : chaque ligne est cliquable
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            groupes.forEachIndexed { i, (label, cents) ->
                val pct = (cents.toFloat() / total * 100).toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onItemClick(label) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = couleurs[i % couleurs.size])
                    }
                    Text(
                        text  = "$label$pct% · ${cents.toCurrencyDisplay(currency)}${trailingLabel(label)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
