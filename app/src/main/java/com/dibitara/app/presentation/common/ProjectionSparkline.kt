package com.dibitara.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dibitara.app.domain.model.CashflowPoint

/**
 * Courbe de trésorerie (aire + ligne), sans axes ni libellés par point : réutilisée en
 * compact sur le Dashboard et en grand sur l'écran de détail Projection. Volontairement
 * sans graduation quotidienne (contrairement à l'ancien graphique Vico du détail, qui
 * tronquait ses libellés de date - voir ProjectionDetailScreen) : ce que la courbe montre,
 * c'est la forme (déclin, plateau, rechute), pas une valeur exacte lue sur un point donné.
 */
@Composable
fun ProjectionSparkline(points: List<CashflowPoint>, color: Color, modifier: Modifier = Modifier) {
    val min = points.minOf { it.soldeCents }
    val max = points.maxOf { it.soldeCents }
    val span = (max - min).takeIf { it != 0L } ?: 1L

    Canvas(modifier = modifier) {
        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
        val line = Path()
        points.forEachIndexed { i, point ->
            val x = i * stepX
            val y = size.height - ((point.soldeCents - min).toFloat() / span) * size.height
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(line, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
