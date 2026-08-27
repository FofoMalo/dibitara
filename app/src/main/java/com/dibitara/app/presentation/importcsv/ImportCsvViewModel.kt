package com.dibitara.app.presentation.importcsv

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.CsvImportPreview
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.usecase.AnalyserCsvUseCase
import com.dibitara.app.domain.usecase.GetBankAccountsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.ImportCsvResult
import com.dibitara.app.domain.usecase.ImporterTransactionsCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'import CSV générique.
 *
 * Cycle :
 *  1. [choisirFichier] lit le fichier et lance l'analyse ([AnalyserCsvUseCase]).
 *     - mapping complet deviné → [ImportCsvUiState.Apercu]
 *     - mapping incomplet      → [ImportCsvUiState.MappingRequis]
 *  2. [appliquerMapping] rejoue l'analyse avec le mapping ajusté par l'utilisateur.
 *  3. [modifierCategorie] / [basculerInclusion] / [choisirCompte] éditent l'aperçu.
 *  4. [confirmer] écrit en base ([ImporterTransactionsCsvUseCase]).
 */
@HiltViewModel
class ImportCsvViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyserCsv: AnalyserCsvUseCase,
    private val importerCsv: ImporterTransactionsCsvUseCase,
    private val getBankAccounts: GetBankAccountsUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportCsvUiState>(ImportCsvUiState.Vide)
    val uiState: StateFlow<ImportCsvUiState> = _uiState.asStateFlow()

    // Conservés entre l'analyse et l'écran de mapping éventuel.
    private var lignesBrutes: List<String> = emptyList()
    private var deviseParDefaut: Currency = Currency.EUR
    private var comptes: List<BankAccount> = emptyList()

    fun choisirFichier(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportCsvUiState.Analyse
            try {
                lignesBrutes = lireLignes(uri)
                if (lignesBrutes.none { it.isNotBlank() }) {
                    _uiState.value = ImportCsvUiState.Erreur("Le fichier est vide.")
                    return@launch
                }
                if (lignesBrutes.size > MAX_LIGNES) {
                    _uiState.value = ImportCsvUiState.Erreur(
                        "Fichier trop volumineux ($MAX_LIGNES lignes maximum). " +
                            "Découpez l'export par année."
                    )
                    return@launch
                }
                deviseParDefaut = getUserPreferences().first().deviseParDefaut
                comptes = getBankAccounts().first()

                analyser(mappingImpose = null)
            } catch (e: Exception) {
                _uiState.value = ImportCsvUiState.Erreur("Impossible de lire le fichier : ${e.message}")
            }
        }
    }

    fun appliquerMapping(mapping: CsvColumnMapping) {
        viewModelScope.launch {
            _uiState.value = ImportCsvUiState.Analyse
            analyser(mappingImpose = mapping)
        }
    }

    private suspend fun analyser(mappingImpose: CsvColumnMapping?) {
        val preview = analyserCsv(lignesBrutes, deviseParDefaut, mappingImpose)
        if (!preview.mapping.estComplet) {
            _uiState.value = ImportCsvUiState.MappingRequis(preview)
            return
        }
        if (preview.transactions.isEmpty()) {
            _uiState.value = ImportCsvUiState.Erreur(
                "Aucune transaction reconnue. Vérifiez le mapping des colonnes."
            )
            return
        }
        val avecDoublons = importerCsv.verifierDoublons(preview.transactions)
        _uiState.value = ImportCsvUiState.Apercu(
            transactions = avecDoublons,
            lignesIgnorees = preview.lignesIgnorees,
            comptes = comptes,
            compteChoisiId = null,
        )
    }

    /** Rouvre l'écran de mapping depuis l'aperçu (bouton « Ajuster les colonnes »). */
    fun ouvrirMapping() {
        viewModelScope.launch {
            _uiState.value = ImportCsvUiState.Analyse
            val preview = analyserCsv(lignesBrutes, deviseParDefaut, mappingImpose = null)
            _uiState.value = ImportCsvUiState.MappingRequis(preview)
        }
    }

    fun modifierCategorie(externalId: String, categorie: Category) {
        majApercu { transactions ->
            transactions.map {
                if (it.externalId == externalId && !it.alreadyImported) it.copy(category = categorie) else it
            }
        }
    }

    fun basculerInclusion(externalId: String) {
        majApercu { transactions ->
            transactions.map {
                if (it.externalId == externalId) it.copy(inclure = !it.inclure) else it
            }
        }
    }

    fun choisirCompte(compteId: Long?) {
        val etat = _uiState.value as? ImportCsvUiState.Apercu ?: return
        _uiState.value = etat.copy(compteChoisiId = compteId)
    }

    fun confirmer() {
        val etat = _uiState.value as? ImportCsvUiState.Apercu ?: return
        viewModelScope.launch {
            _uiState.value = ImportCsvUiState.ImportEnCours
            importerCsv.confirmer(etat.transactions, etat.compteChoisiId).fold(
                onSuccess = { _uiState.value = ImportCsvUiState.Termine(it) },
                onFailure = { _uiState.value = ImportCsvUiState.Erreur("Échec de l'import : ${it.message}") },
            )
        }
    }

    fun reinitialiser() {
        lignesBrutes = emptyList()
        _uiState.value = ImportCsvUiState.Vide
    }

    private inline fun majApercu(transform: (List<ImportedTransaction>) -> List<ImportedTransaction>) {
        val etat = _uiState.value as? ImportCsvUiState.Apercu ?: return
        _uiState.value = etat.copy(transactions = transform(etat.transactions))
    }

    /** Lit le fichier : UTF-8, repli ISO-8859-1 si le décodage produit des caractères de remplacement. */
    private fun lireLignes(uri: Uri): List<String> {
        val octets = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("fichier introuvable")
        val texte = octets.toString(Charsets.UTF_8)
            .let { if (it.contains('�')) octets.toString(Charsets.ISO_8859_1) else it }
        return texte.split(Regex("\r\n|\r|\n"))
    }

    private companion object {
        const val MAX_LIGNES = 20_000
    }
}

sealed interface ImportCsvUiState {
    /** Avant sélection d'un fichier. */
    data object Vide : ImportCsvUiState

    /** Lecture et analyse du fichier en cours. */
    data object Analyse : ImportCsvUiState

    /** L'auto-détection n'a pas suffi : l'utilisateur doit mapper les colonnes. */
    data class MappingRequis(val preview: CsvImportPreview) : ImportCsvUiState

    /** Aperçu éditable avant écriture. */
    data class Apercu(
        val transactions: List<ImportedTransaction>,
        val lignesIgnorees: Int,
        val comptes: List<BankAccount>,
        val compteChoisiId: Long?,
    ) : ImportCsvUiState {
        val nouvelles: Int get() = transactions.count { !it.alreadyImported && it.inclure }
        val doublons: Int get() = transactions.count { it.alreadyImported }
    }

    /** Insertion en base en cours. */
    data object ImportEnCours : ImportCsvUiState

    data class Termine(val resultat: ImportCsvResult) : ImportCsvUiState

    data class Erreur(val message: String) : ImportCsvUiState
}
