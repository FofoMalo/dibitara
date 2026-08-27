package com.dibitara.app.presentation.recommandations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.BudgetBucket
import com.dibitara.app.domain.model.PocheRecommandee
import com.dibitara.app.domain.model.SpendingRecommendation
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommandationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecommandationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recommandations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is RecommandationsUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is RecommandationsUiState.Success ->
                RecommandationsContent(
                    recommandation = state.recommandation,
                    onAppliquerPoche = viewModel::appliquerPoche,
                    onMettreAJourTaux = viewModel::mettreAJourTauxEpargneCible,
                    modifier = Modifier.padding(padding)
                )
        }
    }
}

@Composable
private fun RecommandationsContent(
    recommandation   : SpendingRecommendation,
    onAppliquerPoche : (PocheRecommandee) -> Unit,
    onMettreAJourTaux: (Int) -> Unit,
    modifier         : Modifier = Modifier
) {
    // Dialogue de modification du taux d'épargne cible
    var showTauxDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // En-tête : base de calcul transparente
        item { EnTeteCard(recommandation) }

        // Objectif épargne : actuel vs cible
        item {
            EpargneCard(
                recommandation   = recommandation,
                onModifierTaux   = { showTauxDialog = true }
            )
        }

        // Poches par bucket (Besoins → Envies → Épargne/Dettes)
        BudgetBucket.entries.forEach { bucket ->
            val poches = recommandation.pouchesRecommandees.filter { it.bucket == bucket }
            if (poches.isNotEmpty()) {
                item {
                    Text(
                        text  = "${bucket.displayName} (cible ${bucket.pourcentageCible} %)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(poches) { poche ->
                    PocheCard(
                        poche         = poche,
                        currency      = recommandation.currency,
                        onAppliquer   = { onAppliquerPoche(poche) }
                    )
                }
            }
        }

        // Indicateur de viabilité du plan global
        item { IndicateurViabilitéCard(recommandation) }
    }

    if (showTauxDialog) {
        DialogueTauxEpargne(
            tauxActuel = recommandation.tauxEpargneCiblePct,
            onConfirm  = { pct ->
                onMettreAJourTaux(pct)
                showTauxDialog = false
            },
            onDismiss  = { showTauxDialog = false }
        )
    }
}

// ─── En-tête : transparence de la base de calcul ─────────────────────────────

@Composable
private fun EnTeteCard(recommandation: SpendingRecommendation) {
    val nomsMois = recommandation.moisDeReference.joinToString(", ") { (mois, annee) ->
        Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() } + " $annee"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Base de calcul", style = MaterialTheme.typography.titleSmall)
            Text(
                "Basé sur vos données de $nomsMois.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Revenu moyen", style = MaterialTheme.typography.bodyMedium)
                Text(
                    recommandation.revenuMoyenCents.toCurrencyDisplay(recommandation.currency),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (recommandation.engagementsMensuels > 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Engagements fixes (dettes + épargne programmée)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        "− ${recommandation.engagementsMensuels.toCurrencyDisplay(recommandation.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ─── Carte objectif épargne ───────────────────────────────────────────────────

@Composable
private fun EpargneCard(
    recommandation : SpendingRecommendation,
    onModifierTaux : () -> Unit
) {
    val actuelPct = recommandation.tauxEpargneActuelPct
    val ciblePct  = recommandation.tauxEpargneCiblePct
    val couleur   = when {
        actuelPct == null         -> MaterialTheme.colorScheme.onSurfaceVariant
        actuelPct >= ciblePct     -> Color(0xFF2E7D32)   // vert : objectif atteint
        actuelPct >= ciblePct / 2 -> Color(0xFFE68A00)   // orange : mi-chemin
        else                       -> MaterialTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text("Objectif d'épargne", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onModifierTaux) { Text("Modifier") }
            }

            // Taux actuel vs cible
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Actuel", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (actuelPct != null) "$actuelPct %" else "—",
                        style      = MaterialTheme.typography.headlineSmall,
                        color      = couleur,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cible (règle 50/30/20)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$ciblePct %",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Montant en euros correspondant à l'objectif cible
            Text(
                "Soit ${recommandation.objectifEpargneCents.toCurrencyDisplay(recommandation.currency)}/mois " +
                    "à mettre de côté sur un revenu de " +
                    recommandation.revenuMoyenCents.toCurrencyDisplay(recommandation.currency) + ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Barre de progression
            if (actuelPct != null && ciblePct > 0) {
                LinearProgressIndicator(
                    progress          = { (actuelPct.toFloat() / ciblePct).coerceIn(0f, 1f) },
                    modifier          = Modifier.fillMaxWidth(),
                    color             = couleur,
                    trackColor        = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ─── Carte poche par catégorie ────────────────────────────────────────────────

@Composable
private fun PocheCard(
    poche       : PocheRecommandee,
    currency    : com.dibitara.app.domain.model.Currency,
    onAppliquer : () -> Unit
) {
    val aDejaEnveloppe = poche.enveloppeExistante != null
    val ecart          = poche.ecartAvecEnveloppe

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (aDejaEnveloppe)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(poche.category.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Moyenne 3 mois : ${poche.moyenneCents.toCurrencyDisplay(currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        poche.recommandeCents.toCurrencyDisplay(currency),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("/ mois", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Comparaison avec l'enveloppe existante si elle est configurée
            if (aDejaEnveloppe && ecart != null) {
                val (label, couleur) = when {
                    ecart == 0L -> "Identique à votre enveloppe actuelle" to MaterialTheme.colorScheme.onSurfaceVariant
                    ecart > 0L  -> "+${ecart.toCurrencyDisplay(currency)} vs enveloppe actuelle" to Color(0xFFE68A00)
                    else        -> "${ecart.toCurrencyDisplay(currency)} vs enveloppe actuelle" to Color(0xFF2E7D32)
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = couleur)
            }

            // Bouton "Appliquer" - crée ou met à jour la CategoryEnvelope
            Row(
                horizontalArrangement = Arrangement.End,
                modifier              = Modifier.fillMaxWidth()
            ) {
                if (aDejaEnveloppe) {
                    OutlinedButton(
                        onClick      = onAppliquer,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mettre à jour l'enveloppe", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick      = onAppliquer,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Créer l'enveloppe", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ─── Indicateur de viabilité global ──────────────────────────────────────────

@Composable
private fun IndicateurViabilitéCard(recommandation: SpendingRecommendation) {
    val couleur = if (recommandation.estEquilibre) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val solde   = recommandation.soldePrevisionelCents

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = couleur.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (recommandation.estEquilibre) "Plan équilibré" else "Plan en déficit",
                style      = MaterialTheme.typography.titleSmall,
                color      = couleur,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (recommandation.estEquilibre)
                    "Solde prévisionnel : +${solde.toCurrencyDisplay(recommandation.currency)}/mois"
                else
                    "Les poches suggérées dépassent votre revenu disponible de " +
                        "${(-solde).toCurrencyDisplay(recommandation.currency)}. " +
                        "Réduisez certaines poches pour revenir à l'équilibre.",
                style = MaterialTheme.typography.bodySmall,
                color = couleur
            )
        }
    }
}

// ─── Dialogue taux d'épargne cible ───────────────────────────────────────────

@Composable
private fun DialogueTauxEpargne(
    tauxActuel : Int,
    onConfirm  : (Int) -> Unit,
    onDismiss  : () -> Unit
) {
    var valeur by remember { mutableStateOf(tauxActuel.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Objectif d'épargne") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Définissez le pourcentage de vos revenus que vous souhaitez épargner chaque mois. " +
                        "La règle 50/30/20 recommande 20 %.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value         = valeur,
                    onValueChange = { if (it.length <= 3) valeur = it.filter { c -> c.isDigit() } },
                    label         = { Text("Taux cible (%)") },
                    singleLine    = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pct = valeur.toIntOrNull() ?: return@TextButton
                if (pct in 0..100) onConfirm(pct)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
