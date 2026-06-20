package com.dibitara.app.presentation.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.dibitara.app.domain.usecase.ImportResult
import java.time.format.DateTimeFormatter

/**
 * Écran d'import de transactions TradeRepublic.
 *
 * Flux en 3 étapes :
 *  1. Initial    - l'utilisateur choisit un fichier CSV
 *  2. Preview    - liste des transactions parsées avec statut doublon / nouveau
 *  3. Succès     - résumé : X importées, Y doublons ignorés
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Launcher système pour ouvrir le sélecteur de fichier
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.parserFichier(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import TradeRepublic") },
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

                is ImportUiState.Initial -> EtapeSelection(
                    onChoisirFichier = { filePickerLauncher.launch("*/*") },
                    banqueNom        = "TradeRepublic"
                )

                is ImportUiState.Chargement -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is ImportUiState.Preview -> EtapePreview(
                    transactions      = state.transactions,
                    onConfirmer       = { viewModel.confirmerImport(state.transactions) },
                    onAnnuler         = { viewModel.reinitialiser() },
                    onModifierCategorie = { id, cat -> viewModel.modifierCategorie(id, cat) }
                )

                is ImportUiState.Succes -> EtapeSucces(
                    resultat     = state.resultat,
                    onTerminer   = onNavigateBack,
                    onNouvelImport = { viewModel.reinitialiser() }
                )

                is ImportUiState.Erreur -> EtapeErreur(
                    message    = state.message,
                    onReessayer = { viewModel.reinitialiser() }
                )
            }
        }
    }
}

// ─── Étape 1 : Sélection ─────────────────────────────────────────────────────

@Composable
internal fun EtapeSelection(
    onChoisirFichier: () -> Unit,
    banqueNom: String = "TradeRepublic",
    formatFichier: String = "CSV"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Import de transactions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sélectionnez l'export $formatFichier de votre compte $banqueNom.\nLes doublons sont détectés automatiquement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onChoisirFichier,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Choisir un fichier $formatFichier")
        }
    }
}

// ─── Étape 2 : Preview ───────────────────────────────────────────────────────

@Composable
internal fun EtapePreview(
    transactions: List<ImportedTransaction>,
    onConfirmer: () -> Unit,
    onAnnuler: () -> Unit,
    onModifierCategorie: (externalId: String, category: Category) -> Unit = { _, _ -> }
) {
    val nouvelles = transactions.count { !it.alreadyImported }
    val doublons  = transactions.count {  it.alreadyImported }

    Column(modifier = Modifier.fillMaxSize()) {
        // Résumé en haut
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
                StatChip(label = "Total", valeur = transactions.size.toString(),
                    couleur = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Liste des transactions
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(transactions, key = { it.externalId }) { tx ->
                LigneTransaction(
                    tx = tx,
                    onModifierCategorie = { cat -> onModifierCategorie(tx.externalId, cat) }
                )
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

@Composable
internal fun StatChip(label: String, valeur: String, couleur: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valeur, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = couleur)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// Catégories proposables manuellement (on exclut INVESTISSEMENT et EPARGNE - auto-assignées)
private val CATEGORIES_MANUELLES = Category.entries.filter {
    it != Category.INVESTISSEMENT && it != Category.EPARGNE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LigneTransaction(
    tx: ImportedTransaction,
    onModifierCategorie: (Category) -> Unit = {}
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yy") }
    val montantStr = buildString {
        append(if (tx.type == TransactionType.INCOME) "+" else "-")
        append("%.2f".format(tx.amountCents / 100.0).replace('.', ','))
        append(" ${tx.currency.symbol}")
    }
    val couleurMontant = if (tx.type == TransactionType.INCOME)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    var showCategoryMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = tx.note,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Les doublons sont grisés pour signifier qu'ils ne seront pas importés
                color = if (tx.alreadyImported)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tx.date.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Catégorie éditable - chip tappable avec indicateur crayon
                if (!tx.alreadyImported) {
                    Box {
                        SuggestionChip(
                            onClick = { showCategoryMenu = true },
                            label = {
                                Text(
                                    "${tx.category.displayName} ✎",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            CATEGORIES_MANUELLES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        onModifierCategorie(cat)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        tx.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    montantStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tx.alreadyImported) couleurMontant.copy(alpha = 0.4f) else couleurMontant
                )
                if (tx.alreadyImported) {
                    Text(
                        "Déjà importé",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    )
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─── Étape 3 : Succès ────────────────────────────────────────────────────────

@Composable
internal fun EtapeSucces(
    resultat: ImportResult,
    onTerminer: () -> Unit,
    onNouvelImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("Import terminé !", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "${resultat.importees} transaction(s) importée(s)",
            style = MaterialTheme.typography.bodyLarge
        )
        if (resultat.ignorees > 0) {
            Text(
                "${resultat.ignorees} doublon(s) ignoré(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onTerminer,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Voir mes transactions")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onNouvelImport) {
            Text("Importer un autre fichier")
        }
    }
}

// ─── État d'erreur ────────────────────────────────────────────────────────────

@Composable
internal fun EtapeErreur(message: String, onReessayer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Erreur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onReessayer) {
            Text("Réessayer")
        }
    }
}
