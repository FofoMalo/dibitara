package com.dibitara.app.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Case à cocher + montant signé pour déclarer un mouvement de capital (apport/retrait) plutôt
 * qu'une performance de marché - neutralisé ensuite dans le taux "Acquisition → Aujourd'hui"
 * (voir [AcquisitionEvolutionBlock], `EnregistrerMouvementCapitalUseCase`). Réutilisé sur les
 * 4 sheets d'édition d'actif investissement (Actif libre, Immobilier, SCPI, Épargne salariale).
 *
 * [montantSuggereCents] doit refléter l'écart pertinent pour l'actif édité - un simple delta
 * de valeur pour la plupart des types, mais un delta de PARTS valorisé au prix actuel pour SCPI
 * (voir `EditScpiSheet` : une révision du prix de la part par le gestionnaire est de la
 * performance, pas un mouvement de capital). Le montant saisi suit ce calcul en direct tant
 * que Florent ne le modifie pas lui-même (piège vécu : figé à 0 si la case était cochée avant
 * la saisie de la nouvelle valeur - voir CADRAGE_MOUVEMENTS_CAPITAL_SCPI_EPARGNE.md).
 */
@Composable
fun MouvementCapitalField(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    montant: String,
    onMontantChange: (String) -> Unit,
    montantSuggereCents: Long,
    checkboxLabel: String,
    supportingText: String,
    focusManager: FocusManager
) {
    var montantModifieManuellement by remember { mutableStateOf(false) }

    LaunchedEffect(montantSuggereCents, checked) {
        if (checked && !montantModifieManuellement) {
            onMontantChange("%.2f".format(montantSuggereCents / 100.0).replace(',', '.'))
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = { c ->
            onCheckedChange(c)
            montantModifieManuellement = false
        })
        Text(checkboxLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (checked) {
        OutlinedTextField(
            value = montant,
            onValueChange = { onMontantChange(it); montantModifieManuellement = true },
            label = { Text("Montant du mouvement (+ apport, - retrait)") },
            supportingText = { Text(supportingText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
