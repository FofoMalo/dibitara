package com.dibitara.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.DuplicateGroup
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateCleanupScreen(
    onNavigateBack: () -> Unit,
    viewModel: DuplicateCleanupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is DuplicateCleanupEvent.Supprime -> {
                    snackbarHostState.showSnackbar("Doublons supprimés avec succès")
                    onNavigateBack()
                }
                is DuplicateCleanupEvent.Erreur -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nettoyer les doublons") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DuplicateCleanupUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Analyse en cours…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is DuplicateCleanupUiState.Success -> {
                if (state.groups.isEmpty()) {
                    EtatAucunDoublon(padding)
                } else {
                    val totalASupprimer = state.groups.sumOf { it.transactions.size - 1 }
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        Text(
                            "$totalASupprimer doublon${if (totalASupprimer > 1) "s" else ""} détecté${if (totalASupprimer > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(state.groups) { index, group ->
                                DuplicateGroupCard(
                                    group = group,
                                    onSelectionChange = { keepId -> viewModel.changerSelection(index, keepId) }
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                        // Bouton d'action fixe en bas de l'écran
                        Button(
                            onClick = { viewModel.supprimerDoublons() },
                            enabled = !state.suppressionEnCours,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (state.suppressionEnCours) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Suppression en cours…")
                            } else {
                                Text("Supprimer $totalASupprimer doublon${if (totalASupprimer > 1) "s" else ""}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EtatAucunDoublon(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Aucun doublon détecté", style = MaterialTheme.typography.titleMedium)
            Text(
                "Toutes vos transactions sont uniques.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onSelectionChange: (Long) -> Unit
) {
    val premiereTx = group.transactions.first()
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // En-tête du groupe
            Text(
                "${group.transactions.size} transactions identiques",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "${premiereTx.date.format(formatter).replaceFirstChar { it.uppercase() }}" +
                    " • ${premiereTx.amountCents.toCurrencyDisplay(premiereTx.currency)}" +
                    " • ${premiereTx.type.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Une ligne par transaction : radio + libellé + badge action
            group.transactions.forEach { transaction ->
                val estConservee = transaction.id == group.keepId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RadioButton(
                        selected = estConservee,
                        onClick = { onSelectionChange(transaction.id) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.note.ifBlank { "— sans libellé —" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            buildString {
                                append("id : ${transaction.id}")
                                if (transaction.importSource != null) append(" • ${transaction.importSource}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (estConservee) "Conserver" else "Supprimer",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (estConservee) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
