package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

/**
 * Import CSV en deux phases, pour laisser l'utilisateur valider un aperçu avant
 * toute écriture :
 *  1. [verifierDoublons] — enrichit la liste avec [ImportedTransaction.alreadyImported],
 *     n'insère rien ;
 *  2. [confirmer] — insère les lignes cochées et nouvelles, met à jour la date du
 *     dernier import, retourne le bilan.
 *
 * La déduplication se fait sur [ImportedTransaction.externalId] :
 *  - contre la base (re-vérifiée dans [confirmer] pour tenir compte d'un import
 *    concurrent) ;
 *  - à l'intérieur du fichier lui-même ([distinctBy]) : deux lignes de même date,
 *    montant et libellé produisent le même id et ne comptent qu'une fois. Limite
 *    assumée, voir CADRAGE_SPRINT_44_IMPORT_CSV.md §3.
 */
class ImporterTransactionsCsvUseCase @Inject constructor(
    private val importRepository: ImportRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /** Phase 1 — aperçu. Positionne [ImportedTransaction.alreadyImported]. N'écrit rien. */
    suspend fun verifierDoublons(transactions: List<ImportedTransaction>): List<ImportedTransaction> {
        val existants = importRepository.externalIdsExistants()
        return transactions.map { it.copy(alreadyImported = it.externalId in existants) }
    }

    /**
     * Phase 2 — confirmation. Insère les transactions [ImportedTransaction.inclure]
     * qui ne sont pas déjà en base, sous le compte [bankAccountId] (ou aucun si null).
     * Enveloppé dans [Result] pour que le ViewModel gère l'erreur proprement.
     */
    suspend fun confirmer(
        transactions: List<ImportedTransaction>,
        bankAccountId: Long?,
    ): Result<ImportCsvResult> = runCatching {
        val existants = importRepository.externalIdsExistants()
        val inclus = transactions.filter { it.inclure }
        val aInserer = inclus
            .filter { !it.alreadyImported && it.externalId !in existants }
            .distinctBy { it.externalId }

        val nb = importRepository.importerTransactions(aInserer.map { it.toTransaction(bankAccountId) })
        if (nb > 0) userPreferencesRepository.updateDerniereImport(System.currentTimeMillis())

        ImportCsvResult(importees = nb, doublonsIgnores = inclus.size - nb)
    }
}

/** Bilan d'un import CSV confirmé. */
data class ImportCsvResult(
    val importees: Int,        // transactions effectivement insérées
    val doublonsIgnores: Int,  // lignes cochées mais déjà présentes (base ou doublon interne)
)
