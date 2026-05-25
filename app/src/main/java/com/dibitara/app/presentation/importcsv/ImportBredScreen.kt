package com.dibitara.app.presentation.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Écran d'import de transactions BRED.
 *
 * Réutilise les composables internes d'[ImportScreen] (EtapeSelection, EtapePreview, etc.)
 * mais orchestre [ImportBredViewModel] qui appelle [BredCsvParser].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBredScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportBredViewModel = hiltViewModel()
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
                title = { Text("Import BRED") },
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
                    banqueNom        = "BRED"
                )

                is ImportUiState.Chargement -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is ImportUiState.Preview -> EtapePreview(
                    transactions = state.transactions,
                    onConfirmer  = { viewModel.confirmerImport(state.transactions) },
                    onAnnuler    = { viewModel.reinitialiser() }
                )

                is ImportUiState.Succes -> EtapeSucces(
                    resultat       = state.resultat,
                    onTerminer     = onNavigateBack,
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
