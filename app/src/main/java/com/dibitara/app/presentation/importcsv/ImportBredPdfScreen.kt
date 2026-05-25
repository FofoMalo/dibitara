package com.dibitara.app.presentation.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.data.importcsv.BredPdfParser

/**
 * Écran d'import d'un relevé PDF BRED.
 *
 * Réutilise les composables d'[ImportScreen] (EtapeSelection, EtapePreview, EtapeSucces,
 * EtapeErreur) et ajoute un volet de soldes extrait du PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBredPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportBredPdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.parserFichier(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import PDF BRED") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {

                is ImportBredPdfUiState.Initial -> EtapeSelection(
                    onChoisirFichier = { filePickerLauncher.launch("application/pdf") },
                    banqueNom        = "BRED",
                    formatFichier    = "PDF"
                )

                is ImportBredPdfUiState.Chargement -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is ImportBredPdfUiState.Preview -> PreviewPdf(
                    state      = state,
                    onConfirmer = { viewModel.confirmerImport(state.transactions) },
                    onAnnuler   = { viewModel.reinitialiser() }
                )

                is ImportBredPdfUiState.Succes -> EtapeSucces(
                    resultat       = state.resultat,
                    onTerminer     = onNavigateBack,
                    onNouvelImport = { viewModel.reinitialiser() }
                )

                is ImportBredPdfUiState.Erreur -> EtapeErreur(
                    message    = state.message,
                    onReessayer = { viewModel.reinitialiser() }
                )
            }
        }
    }
}

// ─── Preview avec soldes ──────────────────────────────────────────────────────

@Composable
private fun PreviewPdf(
    state: ImportBredPdfUiState.Preview,
    onConfirmer: () -> Unit,
    onAnnuler: () -> Unit
) {
    val nouvelles = state.transactions.count { !it.alreadyImported }
    val doublons  = state.transactions.count {  it.alreadyImported }

    Column(modifier = Modifier.fillMaxSize()) {
        // Résumé transactions
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(label = "Nouvelles", valeur = nouvelles.toString(),
                    couleur = MaterialTheme.colorScheme.primary)
                StatChip(label = "Doublons", valeur = doublons.toString(),
                    couleur = MaterialTheme.colorScheme.outline)
                StatChip(label = "Total", valeur = state.transactions.size.toString(),
                    couleur = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Soldes extraits du PDF (carte repliable)
            if (state.soldes.isNotEmpty()) {
                item {
                    CarteSoldes(soldes = state.soldes, annee = state.annee)
                }
            }
            // Liste des transactions
            items(state.transactions, key = { it.externalId }) { tx ->
                LigneTransaction(tx)
            }
        }

        // Barre d'actions
        Surface(shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAnnuler,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }
                Button(
                    onClick = onConfirmer,
                    enabled = nouvelles > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (nouvelles > 0) "Importer ($nouvelles)" else "Aucune nouvelle")
                }
            }
        }
    }
}

// ─── Carte soldes ─────────────────────────────────────────────────────────────

@Composable
private fun CarteSoldes(soldes: List<BredPdfParser.SoldeCompte>, annee: Int) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Soldes au relevé $annee",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Réduire" else "Voir (${soldes.size})")
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                soldes.forEach { solde ->
                    LigneSolde(solde)
                }
            }
        }
    }
}

@Composable
private fun LigneSolde(solde: BredPdfParser.SoldeCompte) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = solde.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "%.2f €".format(solde.soldeCents / 100.0).replace('.', ','),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (solde.plafondCents != null) {
                Text(
                    text = "/ %.2f €".format(solde.plafondCents / 100.0).replace('.', ','),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
