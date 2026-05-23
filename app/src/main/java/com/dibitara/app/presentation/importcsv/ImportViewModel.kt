package com.dibitara.app.presentation.importcsv

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.data.importcsv.TradeRepublicCsvParser
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.usecase.ImportResult
import com.dibitara.app.domain.usecase.ImportTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'écran d'import TradeRepublic.
 *
 * Orchestre les 3 étapes :
 *  1. L'utilisateur choisit un fichier CSV → [parserFichier] parse + vérifie les doublons
 *  2. L'utilisateur valide le preview       → [confirmerImport] insère les nouvelles transactions
 *  3. Résultat affiché                      → [reinitialiser] remet à l'état Initial
 *
 * Le contexte est nécessaire pour ouvrir le fichier via ContentResolver.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ucImport: ImportTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Initial)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    /**
     * Lit le fichier désigné par [uri], le parse et marque les doublons.
     * Passe en état [ImportUiState.Preview] si des transactions valides sont trouvées.
     */
    fun parserFichier(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Chargement
            try {
                val transactions = context.contentResolver.openInputStream(uri)?.use { stream ->
                    TradeRepublicCsvParser.parse(stream)
                } ?: emptyList()

                if (transactions.isEmpty()) {
                    _uiState.value = ImportUiState.Erreur(
                        "Aucune transaction valide trouvée. Vérifiez que le fichier est bien un export TradeRepublic."
                    )
                    return@launch
                }

                val avecDoublons = ucImport.verifierDoublons(transactions)
                _uiState.value = ImportUiState.Preview(avecDoublons)

            } catch (e: Exception) {
                _uiState.value = ImportUiState.Erreur("Impossible de lire le fichier : ${e.message}")
            }
        }
    }

    /**
     * Insère en base les transactions non-dupliquées de la liste [transactions].
     * [transactions] peut avoir été modifiée par l'utilisateur dans le preview (catégorie).
     */
    fun confirmerImport(transactions: List<ImportedTransaction>) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Chargement
            ucImport.confirmer(transactions).fold(
                onSuccess = { _uiState.value = ImportUiState.Succes(it) },
                onFailure = { _uiState.value = ImportUiState.Erreur("Erreur lors de l'import : ${it.message}") }
            )
        }
    }

    /** Remet l'écran à l'état initial (sélection de fichier). */
    fun reinitialiser() {
        _uiState.value = ImportUiState.Initial
    }
}

/** États possibles de l'écran d'import. */
sealed class ImportUiState {
    data object Initial                                              : ImportUiState()
    data object Chargement                                           : ImportUiState()
    data class  Preview(val transactions: List<ImportedTransaction>) : ImportUiState()
    data class  Succes(val resultat: ImportResult)                   : ImportUiState()
    data class  Erreur(val message: String)                          : ImportUiState()
}
