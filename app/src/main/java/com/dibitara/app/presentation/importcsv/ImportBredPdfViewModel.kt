package com.dibitara.app.presentation.importcsv

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.data.importcsv.BredPdfParser
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.usecase.ImportResult
import com.dibitara.app.domain.usecase.ImportTransactionsUseCase
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel de l'écran d'import de relevé PDF BRED.
 *
 * Cycle :
 *  1. [parserFichier] : ouvre le PDF via pdfbox, extrait le texte, appelle [BredPdfParser.parseTexte]
 *     puis vérifie les doublons → passe en état [ImportBredPdfUiState.Preview]
 *  2. [confirmerImport] : insère les nouvelles transactions en base
 *  3. [reinitialiser] : remet l'écran à l'état Initial
 */
@HiltViewModel
class ImportBredPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ucImport: ImportTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportBredPdfUiState>(ImportBredPdfUiState.Initial)
    val uiState: StateFlow<ImportBredPdfUiState> = _uiState.asStateFlow()

    fun parserFichier(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportBredPdfUiState.Chargement
            try {
                val resultat = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        PDDocument.load(stream).use { doc ->
                            val stripper = PDFTextStripper().apply { sortByPosition = true }
                            val texte = stripper.getText(doc)
                            BredPdfParser.parseTexte(texte)
                        }
                    }
                }

                if (resultat == null || resultat.transactions.isEmpty()) {
                    _uiState.value = ImportBredPdfUiState.Erreur(
                        "Aucune transaction trouvée dans ce PDF.\n" +
                        "Vérifiez que le fichier est un relevé BRED (compte poste principal)."
                    )
                    return@launch
                }

                val avecDoublons = ucImport.verifierDoublons(resultat.transactions)
                _uiState.value = ImportBredPdfUiState.Preview(
                    transactions = avecDoublons,
                    soldes       = resultat.soldes,
                    annee        = resultat.annee
                )

            } catch (e: Exception) {
                _uiState.value = ImportBredPdfUiState.Erreur(
                    "Impossible de lire le PDF : ${e.message}"
                )
            }
        }
    }

    fun confirmerImport(transactions: List<ImportedTransaction>) {
        viewModelScope.launch {
            _uiState.value = ImportBredPdfUiState.Chargement
            ucImport.confirmer(transactions).fold(
                onSuccess = { _uiState.value = ImportBredPdfUiState.Succes(it) },
                onFailure = { _uiState.value = ImportBredPdfUiState.Erreur("Erreur lors de l'import : ${it.message}") }
            )
        }
    }

    fun reinitialiser() {
        _uiState.value = ImportBredPdfUiState.Initial
    }
}

/** États possibles de l'écran d'import PDF BRED. */
sealed class ImportBredPdfUiState {
    data object Initial    : ImportBredPdfUiState()
    data object Chargement : ImportBredPdfUiState()
    data class Preview(
        val transactions : List<ImportedTransaction>,
        val soldes       : List<BredPdfParser.SoldeCompte>,
        val annee        : Int
    ) : ImportBredPdfUiState()
    data class Succes(val resultat: ImportResult) : ImportBredPdfUiState()
    data class Erreur(val message: String)        : ImportBredPdfUiState()
}
