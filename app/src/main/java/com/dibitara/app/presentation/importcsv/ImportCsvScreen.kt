package com.dibitara.app.presentation.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.presentation.common.chartColor
import com.dibitara.app.presentation.common.formatCurrency

/**
 * Import d'un relevé bancaire au format CSV, quelle que soit la banque.
 *
 * Flux : sélection du fichier → (mapping des colonnes si l'auto-détection échoue)
 * → aperçu éditable → confirmation. Voir [ImportCsvViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportCsvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.choisirFichier(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importer un relevé CSV") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                ImportCsvUiState.Vide ->
                    EtapeSelection(onChoisir = { picker.launch("*/*") })

                ImportCsvUiState.Analyse, ImportCsvUiState.ImportEnCours ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                is ImportCsvUiState.MappingRequis ->
                    ImportCsvMappingScreen(
                        preview = state.preview,
                        onValider = viewModel::appliquerMapping,
                        onAnnuler = viewModel::reinitialiser,
                    )

                is ImportCsvUiState.Apercu ->
                    EtapeApercu(
                        state = state,
                        onModifierCategorie = viewModel::modifierCategorie,
                        onBasculerInclusion = viewModel::basculerInclusion,
                        onChoisirCompte = viewModel::choisirCompte,
                        onAjusterColonnes = viewModel::ouvrirMapping,
                        onConfirmer = viewModel::confirmer,
                        onAnnuler = viewModel::reinitialiser,
                    )

                is ImportCsvUiState.Termine ->
                    EtapeTermine(
                        importees = state.resultat.importees,
                        doublonsIgnores = state.resultat.doublonsIgnores,
                        onTerminer = onNavigateBack,
                    )

                is ImportCsvUiState.Erreur ->
                    EtapeErreur(message = state.message, onReessayer = viewModel::reinitialiser)
            }
        }
    }
}

// ─── Étape 1 : sélection du fichier ──────────────────────────────────────────

@Composable
private fun EtapeSelection(onChoisir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Filled.UploadFile,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Importer vos transactions",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Sélectionnez le fichier CSV exporté depuis votre banque. " +
                "Les colonnes sont détectées automatiquement ; vous pourrez les ajuster " +
                "et vérifier chaque ligne avant l'import.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onChoisir, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choisir un fichier CSV")
        }
        HorizontalDivider()
        Text("Comment obtenir un CSV", style = MaterialTheme.typography.titleSmall)
        Text(
            "• Depuis le site de votre banque : rubrique « Télécharger / Exporter les opérations », " +
                "format CSV ou Excel.\n" +
                "• Depuis Excel, Numbers ou Google Sheets : Fichier → Enregistrer sous / Télécharger → CSV.\n" +
                "• Les transactions identiques du même jour sont comptées une seule fois.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Étape 3 : aperçu éditable ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeApercu(
    state: ImportCsvUiState.Apercu,
    onModifierCategorie: (ligneIndex: Int, Category) -> Unit,
    onBasculerInclusion: (ligneIndex: Int) -> Unit,
    onChoisirCompte: (Long?) -> Unit,
    onAjusterColonnes: () -> Unit,
    onConfirmer: () -> Unit,
    onAnnuler: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                StatChip("Nouvelles", state.nouvelles.toString(), MaterialTheme.colorScheme.primary)
                StatChip("Doublons", state.doublons.toString(), MaterialTheme.colorScheme.outline)
                if (state.lignesIgnorees > 0) {
                    StatChip("Ignorées", state.lignesIgnorees.toString(), MaterialTheme.colorScheme.outline)
                }
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    CompteDropdown(
                        comptes = state.comptes,
                        choisiId = state.compteChoisiId,
                        onChoisir = onChoisirCompte,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onAjusterColonnes, contentPadding = PaddingValues(0.dp)) {
                        Text("Ajuster les colonnes")
                    }
                }
            }
            items(state.transactions, key = { it.ligneIndex }) { tx ->
                LigneApercu(
                    tx = tx,
                    onCategorie = { onModifierCategorie(tx.ligneIndex, it) },
                    onToggle = { onBasculerInclusion(tx.ligneIndex) },
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Surface(shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) { Text("Annuler") }
                Button(
                    onClick = onConfirmer,
                    enabled = state.nouvelles > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.nouvelles > 0) "Importer (${state.nouvelles})" else "Rien à importer")
                }
            }
        }
    }
}

@Composable
private fun LigneApercu(
    tx: ImportedTransaction,
    onCategorie: (Category) -> Unit,
    onToggle: () -> Unit,
) {
    val actif = tx.inclure && !tx.alreadyImported
    val alpha = if (actif) 1f else 0.45f

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = tx.inclure && !tx.alreadyImported,
            onCheckedChange = { onToggle() },
            enabled = !tx.alreadyImported,
        )
        Column(Modifier.weight(1f)) {
            Text(
                tx.note.ifBlank { "(sans libellé)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tx.date.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tx.alreadyImported) {
                    Text(
                        "déjà importée",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
            val signe = if (tx.type == TransactionType.EXPENSE) "-" else "+"
            Text(
                "$signe ${tx.amountCents.formatCurrency(tx.currency)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (tx.type == TransactionType.INCOME) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            CategoriePicker(
                courante = tx.category,
                enabled = actif,
                onChoisir = onCategorie,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriePicker(courante: Category, enabled: Boolean, onChoisir: (Category) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { if (enabled) ouvert = true },
            enabled = enabled,
            label = { Text(courante.displayName, style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Surface(
                    color = courante.chartColor(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(12.dp),
                ) {}
            },
        )
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            Category.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.displayName) },
                    onClick = { onChoisir(cat); ouvert = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompteDropdown(
    comptes: List<com.dibitara.app.domain.model.BankAccount>,
    choisiId: Long?,
    onChoisir: (Long?) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    val libelle = comptes.firstOrNull { it.id == choisiId }?.label ?: "Aucun compte"

    ExposedDropdownMenuBox(expanded = ouvert, onExpandedChange = { ouvert = it }) {
        OutlinedTextField(
            value = libelle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Rattacher au compte") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ouvert) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            DropdownMenuItem(
                text = { Text("Aucun compte") },
                onClick = { onChoisir(null); ouvert = false },
            )
            comptes.forEach { compte ->
                DropdownMenuItem(
                    text = { Text(compte.label) },
                    onClick = { onChoisir(compte.id); ouvert = false },
                )
            }
        }
    }
}

// ─── Étape 4 : résultat ─────────────────────────────────────────────────────

@Composable
private fun EtapeTermine(importees: Int, doublonsIgnores: Int, onTerminer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(16.dp))
        Text("$importees transaction(s) importée(s)", style = MaterialTheme.typography.titleMedium)
        if (doublonsIgnores > 0) {
            Text(
                "$doublonsIgnores doublon(s) ignoré(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onTerminer) { Text("Terminer") }
    }
}

@Composable
private fun EtapeErreur(message: String, onReessayer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onReessayer) { Text("Réessayer") }
    }
}

@Composable
internal fun StatChip(label: String, valeur: String, couleur: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valeur, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = couleur)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
