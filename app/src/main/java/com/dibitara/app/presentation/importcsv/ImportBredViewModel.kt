package com.dibitara.app.presentation.importcsv

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.data.importcsv.BredCsvParser
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.usecase.CategoriseurLibelle
import com.dibitara.app.domain.usecase.GetRuleForNoteUseCase
import com.dibitara.app.domain.usecase.ImportTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'écran d'import BRED.
 *
 * Même cycle que [ImportViewModel] (TradeRepublic) mais utilise [BredCsvParser] :
 *  1. L'utilisateur choisit un fichier CSV → [parserFichier] parse + vérifie les doublons
 *  2. L'utilisateur valide le preview       → [confirmerImport] insère les nouvelles transactions
 *  3. Résultat affiché                      → [reinitialiser] remet à l'état Initial
 */
@HiltViewModel
class ImportBredViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ucImport: ImportTransactionsUseCase,
    private val ucGetRule: GetRuleForNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Initial)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun parserFichier(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Chargement
            try {
                val transactions = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BredCsvParser.parse(stream)
                } ?: emptyList()

                if (transactions.isEmpty()) {
                    _uiState.value = ImportUiState.Erreur(
                        "Aucune transaction valide trouvée. " +
                        "Vérifiez que le fichier est bien un export BRED " +
                        "(CSV séparateur point-virgule, format DD/MM/YYYY)."
                    )
                    return@launch
                }

                val avecDoublons = ucImport.verifierDoublons(transactions)
                _uiState.value = ImportUiState.Preview(recategoriserAuto(avecDoublons))

            } catch (e: Exception) {
                _uiState.value = ImportUiState.Erreur("Impossible de lire le fichier : ${e.message}")
            }
        }
    }

    /**
     * Met à jour la catégorie d'une transaction dans le preview.
     * N'affecte pas les transactions déjà importées ([alreadyImported] = true).
     */
    fun modifierCategorie(externalId: String, nouvelleCategorie: Category) {
        val currentState = _uiState.value
        if (currentState !is ImportUiState.Preview) return
        val updated = currentState.transactions.map { tx ->
            if (tx.externalId == externalId && !tx.alreadyImported) {
                tx.copy(category = nouvelleCategorie)
            } else tx
        }
        _uiState.value = ImportUiState.Preview(updated)
    }

    fun confirmerImport(transactions: List<ImportedTransaction>) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Chargement
            ucImport.confirmer(transactions).fold(
                onSuccess = { _uiState.value = ImportUiState.Succes(it) },
                onFailure = { _uiState.value = ImportUiState.Erreur("Erreur lors de l'import : ${it.message}") }
            )
        }
    }

    fun reinitialiser() {
        _uiState.value = ImportUiState.Initial
    }

    private suspend fun recategoriserAuto(transactions: List<ImportedTransaction>): List<ImportedTransaction> =
        transactions.map { tx ->
            if (!tx.alreadyImported && tx.category == Category.AUTRE) {
                // 1. Règle apprise par l'utilisateur (priorité absolue — exact match)
                val regle = ucGetRule(tx.note)
                if (regle != null) return@map tx.copy(category = regle.category)
                // 2. Dictionnaire générique
                val suggestion = CategoriseurLibelle.suggererCategorie(tx.note)
                if (suggestion != null) tx.copy(category = suggestion) else tx
            } else tx
        }
}
