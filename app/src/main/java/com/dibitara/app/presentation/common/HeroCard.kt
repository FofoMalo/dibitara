package com.dibitara.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HeroShape = RoundedCornerShape(20.dp)

/**
 * Carte "hero" à accent doré en filet, pas en aplat : fond neutre [surface],
 * bordure dorée légère + bande dégradée en haut. Remplace le pattern
 * `containerColor = primaryContainer` utilisé auparavant sur les cartes clés
 * (patrimoine, budget, épargne), qui rendait le texte peu lisible ("or sur or").
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    val stripe = Brush.horizontalGradient(
        listOf(gold, MaterialTheme.colorScheme.secondary, Color.Transparent)
    )
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, gold.copy(alpha = 0.22f))

    val body: @Composable ColumnScope.() -> Unit = {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(stripe)
        )
        content()
    }

    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = HeroShape,
            colors = colors,
            border = border,
            content = body
        )
    } else {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            shape = HeroShape,
            colors = colors,
            border = border,
            content = body
        )
    }
}
