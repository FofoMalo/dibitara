package com.dibitara.app.presentation.scenarios.conseillerpatrimoine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.CategoriePatrimoine
import com.dibitara.app.domain.model.ConseilPatrimoineResult
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PocheAvecMarge
import com.dibitara.app.domain.model.RepartitionCategorie
import com.dibitara.app.presentation.common.toCurrencyDisplay

private val VertTenable = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConseillerPatrimoineScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConseillerPatrimoineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conseiller patrimoine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ConseillerPatrimoineUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is ConseillerPatrimoineUiState.Success ->
                ConseillerPatrimoineContent(state.conseil, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ConseillerPatrimoineContent(conseil: ConseilPatrimoineResult, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { PrecautionCard(conseil) }
        if (conseil.pochesAvecMarge.isNotEmpty()) {
            item { PochesAvecMargeCard(conseil.pochesAvecMarge, conseil.currency) }
        }
        item { CapaciteCard(conseil) }
        item { ConcentrationCard(conseil) }
    }
}

// ─── Axe 1 : épargne de précaution ─────────────────────────────────────────────

@Composable
private fun PrecautionCard(conseil: ConseilPatrimoineResult) {
    val couleur = if (conseil.precautionSuffisante) VertTenable else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Épargne de précaution", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (conseil.precautionSuffisante) "Suffisante" else "À renforcer",
                    style = MaterialTheme.typography.labelLarge,
                    color = couleur,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Comptes courants + Livret A/LDDS, comparés à 6 mois de dépenses et dettes réelles (hors crédit immobilier, déjà compté dans les dépenses).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Liquidités sûres", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        conseil.liquiditesSuresCents.toCurrencyDisplay(conseil.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        color = couleur,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Objectif (6 mois)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        conseil.objectifPrecautionCents.toCurrencyDisplay(conseil.currency),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

// ─── Axe 2 : poches avec marge avant plafond ───────────────────────────────────

@Composable
private fun PochesAvecMargeCard(poches: List<PocheAvecMarge>, currency: Currency) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Poches avec marge avant plafond", style = MaterialTheme.typography.titleSmall)
            Text(
                "Comptes avec un plafond renseigné, triés par marge décroissante - à toi de juger l'ordre selon tes objectifs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            poches.forEach { poche ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(poche.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${poche.soldeCents.toCurrencyDisplay(currency)} / ${poche.plafondCents.toCurrencyDisplay(currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "+ ${poche.margeCents.toCurrencyDisplay(currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VertTenable
                    )
                }
            }
        }
    }
}

// ─── Axe 3 : capacité d'épargne mensuelle ──────────────────────────────────────

@Composable
private fun CapaciteCard(conseil: ConseilPatrimoineResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Capacité d'épargne mensuelle", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Objectif 20%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        conseil.objectifEpargneMensuelCents.toCurrencyDisplay(conseil.currency),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Déjà programmé", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        conseil.versementsProgrammesCents.toCurrencyDisplay(conseil.currency),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (conseil.objectifPlafonneParResteAVivre) {
                Text(
                    "Objectif 20% non atteignable avec le revenu actuel (reste à vivre réel : " +
                        conseil.resteAVivreReelCents.toCurrencyDisplay(conseil.currency) +
                        ") - capacité réaliste utilisée à la place.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (conseil.capaciteNonAffecteeCents > 0) {
                Text(
                    "Capacité non affectée : ${conseil.capaciteNonAffecteeCents.toCurrencyDisplay(conseil.currency)}/mois.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VertTenable
                )
            } else {
                Text(
                    "Capacité réaliste déjà entièrement affectée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Axe 4 : concentration par catégorie ───────────────────────────────────────

@Composable
private fun ConcentrationCard(conseil: ConseilPatrimoineResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Concentration du patrimoine", style = MaterialTheme.typography.titleSmall)
            Text(
                "Immobilier compté net du crédit lié.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            conseil.repartitionParCategorie
                .filter { it.montantCents > 0 }
                .sortedByDescending { it.pourcentage }
                .forEach { ligne -> LigneRepartition(ligne, conseil.currency) }

            if (conseil.categorieSurConcentree != null) {
                Text(
                    "${conseil.categorieSurConcentree.categorie.displayName} représente plus de 70% du patrimoine - concentration à surveiller.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LigneRepartition(ligne: RepartitionCategorie, currency: Currency) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(ligne.categorie.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${ligne.montantCents.toCurrencyDisplay(currency)} (${"%.0f".format(ligne.pourcentage)} %)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress   = { (ligne.pourcentage / 100f).coerceIn(0f, 1f) },
            modifier   = Modifier.fillMaxWidth().padding(top = 4.dp),
            color      = if (ligne.pourcentage > 70f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
