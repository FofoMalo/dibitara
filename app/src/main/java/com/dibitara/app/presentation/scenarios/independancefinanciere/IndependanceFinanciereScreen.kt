package com.dibitara.app.presentation.scenarios.independancefinanciere

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.CapIndependanceFinanciere
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.NatureTemporelle
import com.dibitara.app.domain.model.PocheRecommandee
import com.dibitara.app.presentation.common.toCurrencyDisplay

private val Or       = Color(0xFFE6C675)
private val Vert     = Color(0xFF2E7D32)
private val Orange   = Color(0xFFE68A00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndependanceFinanciereScreen(
    onNavigateBack: () -> Unit,
    viewModel: IndependanceFinanciereViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indépendance financière") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is IndependanceFinanciereUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is IndependanceFinanciereUiState.Success ->
                if (state.cap == null) {
                    AucunSnapshotContent(Modifier.padding(padding))
                } else {
                    IndependanceFinanciereContent(
                        cap = state.cap,
                        poches = state.poches,
                        onMultipleChange = viewModel::mettreAJourMultiple,
                        onRendementChange = viewModel::mettreAJourRendement,
                        modifier = Modifier.padding(padding)
                    )
                }
        }
    }
}

@Composable
private fun AucunSnapshotContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Ouvre l'écran Patrimoine au moins une fois pour créer un premier point de " +
                "mesure - le cap se base sur cet historique.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IndependanceFinanciereContent(
    cap: CapIndependanceFinanciere,
    poches: List<PocheRecommandee>,
    onMultipleChange: (Int) -> Unit,
    onRendementChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Le cap est calculé sur ta dépense annuelle lissée sur 3 mois - pas sur le " +
                    "dernier mois, qui peut être un accident.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { CapCard(cap) }
        item { ReglagesCard(cap, onMultipleChange, onRendementChange) }
        item { EcheanceCard(cap) }
        item {
            Text(
                "Signal vs bruit — mouvements du budget",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Text(
                "Signal = dépense présente chaque mois analysé (dérive durable, mérite d'ajuster " +
                    "le budget). Bruit = présente sur un seul mois (accident ponctuel, rien à faire). " +
                    "« À vérifier » prime sur les deux : une catégorie concentrée sur 1-2 grosses " +
                    "transactions peut imiter l'un ou l'autre sans être fiable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // key doit être un type stockable dans un Bundle (String, primitif...) - un enum brut
        // fait planter la recomposition différée de LazyColumn (prefetch), d'où .name.
        items(poches, key = { it.category.name }) { poche -> PocheNatureCard(poche, cap.currency) }
    }
}

// ─── Cap et progression ─────────────────────────────────────────────────────────

@Composable
private fun CapCard(cap: CapIndependanceFinanciere) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Capital cible (${cap.multipleCible}×)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        cap.capitalCibleCents.toCurrencyDisplay(cap.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${cap.progressionPct} %",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Or
                )
            }
            LinearProgressIndicator(
                progress   = { (cap.progressionPct / 100f).coerceIn(0f, 1f) },
                modifier   = Modifier.fillMaxWidth(),
                color      = Or,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${cap.patrimoineNetCents.toCurrencyDisplay(cap.currency)} atteints sur " +
                    "${cap.capitalCibleCents.toCurrencyDisplay(cap.currency)} · dépense lissée " +
                    "${cap.depenseAnnuelleLisseeCents.toCurrencyDisplay(cap.currency)}/an",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Réglages (multiple, rendement espéré) ───────────────────────────────────────

@Composable
private fun ReglagesCard(
    cap: CapIndependanceFinanciere,
    onMultipleChange: (Int) -> Unit,
    onRendementChange: (Int) -> Unit
) {
    // État local pendant le glissement du curseur - écrit dans les préférences seulement
    // au relâchement (onValueChangeFinished), pas à chaque pixel de déplacement.
    var multiple by remember(cap.multipleCible) { mutableFloatStateOf(cap.multipleCible.toFloat()) }
    var rendement by remember(cap.rendementEspereAnnuelPct) { mutableFloatStateOf(cap.rendementEspereAnnuelPct.toFloat()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Multiple de dépenses (règle des 4 % = 25×)", style = MaterialTheme.typography.bodyMedium)
                Text("${multiple.toInt()}×", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = multiple,
                onValueChange = { multiple = it },
                onValueChangeFinished = { onMultipleChange(multiple.toInt()) },
                valueRange = 15f..40f,
                steps = 24
            )

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Rendement annuel espéré", style = MaterialTheme.typography.bodyMedium)
                Text("${rendement.toInt()} %", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = rendement,
                onValueChange = { rendement = it },
                onValueChangeFinished = { onRendementChange(rendement.toInt()) },
                valueRange = 0f..15f,
                steps = 14
            )
        }
    }
}

// ─── Échéance estimée ────────────────────────────────────────────────────────────

@Composable
private fun EcheanceCard(cap: CapIndependanceFinanciere) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Or.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "À ce rythme",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val mois = cap.moisRestantsEstimes
            Text(
                when {
                    mois == null -> "Non atteignable au rythme actuel"
                    mois == 0    -> "Cap déjà atteint"
                    else         -> "Indépendance dans ~${"%.1f".format(mois / 12f)} ans"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${cap.versementMensuelCents.toCurrencyDisplay(cap.currency)}/mois déjà programmés, " +
                    "rendement ${cap.rendementEspereAnnuelPct} % composé — simulation indicative.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Signal / bruit / concentration par catégorie ───────────────────────────────

@Composable
private fun PocheNatureCard(poche: PocheRecommandee, currency: Currency) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(poche.category.displayName, style = MaterialTheme.typography.titleSmall)
                NatureBadge(poche)
            }
            Text(
                "Moyenne 3 mois : ${poche.moyenneCents.toCurrencyDisplay(currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NatureBadge(poche: PocheRecommandee) {
    // La concentration (F3) prime sur le signal/bruit (F4) - voir
    // PocheRecommandee.natureTemporelle.
    if (poche.aVerifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
            Text("À vérifier", style = MaterialTheme.typography.labelSmall, color = Orange, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    val (label, couleur) = when (poche.natureTemporelle) {
        NatureTemporelle.SIGNAL      -> "Signal" to MaterialTheme.colorScheme.error
        NatureTemporelle.BRUIT       -> "Bruit" to MaterialTheme.colorScheme.onSurfaceVariant
        NatureTemporelle.INDETERMINE -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    if (poche.natureTemporelle == NatureTemporelle.INDETERMINE) {
        Icon(Icons.Default.HelpOutline, contentDescription = label, tint = couleur, modifier = Modifier.size(14.dp))
    } else {
        Text(label, style = MaterialTheme.typography.labelSmall, color = couleur, fontWeight = FontWeight.SemiBold)
    }
}
