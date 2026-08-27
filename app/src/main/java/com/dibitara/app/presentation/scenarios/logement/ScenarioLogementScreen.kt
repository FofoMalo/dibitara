package com.dibitara.app.presentation.scenarios.logement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.CapaciteLogementResult
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PocheReduction
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScenarioLogementProjection
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.format.TextStyle
import java.util.Locale

private val VertTenable = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioLogementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScenarioLogementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scénario logement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ScenarioLogementUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is ScenarioLogementUiState.AucunBien ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        "Ajoute d'abord un bien immobilier dans Placements pour simuler ce scénario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            is ScenarioLogementUiState.Success ->
                ScenarioLogementContent(
                    state              = state,
                    onSelectionnerBien = viewModel::selectionnerBien,
                    onMontantChange    = viewModel::mettreAJourLigneRevenu,
                    onAjouterLigne     = viewModel::ajouterLigneRevenu,
                    modifier           = Modifier.padding(padding)
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioLogementContent(
    state              : ScenarioLogementUiState.Success,
    onSelectionnerBien : (Long) -> Unit,
    onMontantChange    : (Int, Long) -> Unit,
    onAjouterLigne     : () -> Unit,
    modifier           : Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (state.biens.size > 1) {
            item {
                SelecteurBien(
                    biens        = state.biens,
                    selectionne  = state.bienSelectionne,
                    onSelectionner = onSelectionnerBien
                )
            }
        }
        item {
            Text(
                "Revenus simulés",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(state.lignesRevenu, key = { it.id }) { ligne ->
            LigneRevenuField(
                ligne     = ligne,
                currency  = state.capacite.currency,
                onChange  = { montant -> onMontantChange(ligne.id, montant) }
            )
        }
        item {
            TextButton(onClick = onAjouterLigne) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajouter une source de revenu")
            }
        }

        item { RevenuPlancherCard(state.capacite) }
        item { ImpactCard(state.capacite) }

        if (state.capacite.pochesEnviesReduction.isNotEmpty() || !state.capacite.estTenable) {
            item { OuAgirCard(state.capacite) }
        }

        item { ProjectionCard(state.projection) }
    }
}

// ─── Sélecteur de bien (si plusieurs biens enregistrés) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurBien(
    biens         : List<RealEstateAsset>,
    selectionne   : RealEstateAsset,
    onSelectionner: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectionne.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Bien concerné") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            biens.forEach { bien ->
                DropdownMenuItem(
                    text = { Text(bien.label) },
                    onClick = { onSelectionner(bien.id); expanded = false }
                )
            }
        }
    }
}

// ─── Ligne de saisie d'une source de revenu simulée ───────────────────────────

@Composable
private fun LigneRevenuField(
    ligne    : LigneRevenu,
    currency : Currency,
    onChange : (Long) -> Unit
) {
    var texte by remember(ligne.id) {
        mutableStateOf(if (ligne.montantCents == 0L) "" else (ligne.montantCents / 100.0).toString())
    }
    OutlinedTextField(
        value = texte,
        onValueChange = { saisie ->
            texte = saisie
            val cents = saisie.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
            onChange(cents)
        },
        label = { Text("${ligne.label} (${currency.symbol}/mois)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// ─── Revenu plancher : transparence de chaque composant ───────────────────────

@Composable
private fun RevenuPlancherCard(capacite: CapaciteLogementResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Revenu plancher", style = MaterialTheme.typography.titleSmall)
            Text(
                "Le revenu minimum pour tenir, à comparer directement à une offre d'emploi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (capacite.mensualiteMaisonCents != null) {
                LigneDetail(
                    "Mensualité du crédit (repère)",
                    capacite.mensualiteMaisonCents.toCurrencyDisplay(capacite.currency),
                    accent = false
                )
            } else {
                Text(
                    "Aucun crédit lié à ce bien.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            LigneDetail(
                "Besoins réels (moyenne 3 mois, logement inclus)",
                capacite.besoinsIncompressiblesCents.toCurrencyDisplay(capacite.currency)
            )
            LigneDetail(
                "Autres engagements (dettes + épargne programmée)",
                capacite.autresEngagementsCents.toCurrencyDisplay(capacite.currency)
            )

            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Revenu plancher", style = MaterialTheme.typography.titleSmall)
                Text(
                    capacite.revenuPlancherCents.toCurrencyDisplay(capacite.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LigneDetail(label: String, valeur: String, accent: Boolean = true) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            valeur,
            style = MaterialTheme.typography.bodySmall,
            color = if (accent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Impact du scénario simulé ─────────────────────────────────────────────────

@Composable
private fun ImpactCard(capacite: CapaciteLogementResult) {
    val couleur = if (capacite.estTenable) VertTenable else MaterialTheme.colorScheme.error
    val ecartCents = capacite.resteAVivreSimuleCents - capacite.resteAVivreReelCents

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Impact", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (capacite.estTenable) "Tenable" else "Tendu",
                    style = MaterialTheme.typography.labelLarge,
                    color = couleur,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reste à vivre simulé", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        capacite.resteAVivreSimuleCents.toCurrencyDisplay(capacite.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        color = couleur,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reste à vivre actuel", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        capacite.resteAVivreReelCents.toCurrencyDisplay(capacite.currency),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Text(
                "Écart avec la situation actuelle : " +
                    (if (ecartCents >= 0) "+" else "") + ecartCents.toCurrencyDisplay(capacite.currency) + "/mois.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Où agir si le scénario est tendu ──────────────────────────────────────────

@Composable
private fun OuAgirCard(capacite: CapaciteLogementResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Où agir", style = MaterialTheme.typography.titleSmall)

            if (capacite.pochesEnviesReduction.isEmpty()) {
                Text(
                    "Aucune poche \"Envies\" identifiée à réduire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                capacite.pochesEnviesReduction.forEach { poche -> LignePocheReduction(poche, capacite.currency) }
            }

            if (capacite.ecartNonCouvertCents > 0) {
                Text(
                    "Même en réduisant ces poches de moitié, il manque encore " +
                        capacite.ecartNonCouvertCents.toCurrencyDisplay(capacite.currency) + "/mois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LignePocheReduction(poche: PocheReduction, currency: Currency) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(poche.category.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Moyenne actuelle : ${poche.moyenneCents.toCurrencyDisplay(currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "− ${poche.montantACouperCents.toCurrencyDisplay(currency)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// ─── Projection sur plusieurs mois ─────────────────────────────────────────────

@Composable
private fun ProjectionCard(projection: ScenarioLogementProjection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Projection sur ${projection.points.size} mois", style = MaterialTheme.typography.titleSmall)
            Text(
                "Cumul du reste à vivre simulé, mois après mois - revenu et besoins tenus constants.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            projection.points.forEach { point ->
                val positif = point.soldeCents >= 0
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        point.date.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                            .replaceFirstChar { it.uppercase() } + " ${point.date.year}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        point.soldeCents.toCurrencyDisplay(projection.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (positif) VertTenable else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (projection.moisPassageNegatif != null) {
                Text(
                    "Le cumul passe en négatif à partir de " +
                        projection.moisPassageNegatif.month.getDisplayName(TextStyle.FULL, Locale.FRENCH) + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
