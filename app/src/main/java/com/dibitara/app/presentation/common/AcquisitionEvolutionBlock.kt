package com.dibitara.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dibitara.app.domain.model.Currency

/**
 * Bloc "VALEUR DE DÉPART → VALEUR ACTUELLE" (refonte UX/UI, écran Placements) : montre
 * l'évolution d'un actif depuis son acquisition plutôt qu'une simple date de dernière
 * modification. Réutilisé sur les 4 types d'actifs investissement (Immobilier, SCPI,
 * Actif libre, Épargne salariale) - voir [TrendChip] pour le badge de tendance réutilisé
 * en bas du bloc.
 */
@Composable
fun AcquisitionEvolutionBlock(
    acquisitionValueCents: Long,
    currentValueCents: Long,
    currency: Currency,
    deltaCents: Long,
    deltaPct: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "VALEUR DE DÉPART → VALEUR ACTUELLE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Acquisition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(acquisitionValueCents.toCurrencyDisplay(currency), style = MaterialTheme.typography.bodyLarge)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("Aujourd'hui", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    currentValueCents.toCurrencyDisplay(currency),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${if (deltaCents >= 0) "+" else ""}${deltaCents.toCurrencyDisplay(currency)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (deltaCents >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            TrendChip(deltaPct)
        }
    }
}
