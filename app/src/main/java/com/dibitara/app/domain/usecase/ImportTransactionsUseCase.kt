package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.repository.ImportRepository
import javax.inject.Inject

/**
 * Gère l'import en deux phases :
 *  1. [verifierDoublons] — preview sans écriture : enrichit la liste avec [ImportedTransaction.alreadyImported]
 *  2. [confirmer]        — écriture réelle : insère uniquement les transactions nouvelles
 *
 * La séparation en deux étapes permet d'afficher un écran de preview à l'utilisateur
 * avant de toucher la base de données.
 */
class ImportTransactionsUseCase @Inject constructor(
    private val repository: ImportRepository
) {
    /**
     * Étape 1 — Preview.
     * Compare les [ImportedTransaction.externalId] avec ceux déjà en base et
     * retourne la liste avec [ImportedTransaction.alreadyImported] positionné.
     * N'insère rien.
     */
    suspend fun verifierDoublons(transactions: List<ImportedTransaction>): List<ImportedTransaction> {
        val existants = repository.externalIdsExistants()
        return transactions.map { it.copy(alreadyImported = it.externalId in existants) }
    }

    /**
     * Étape 2 — Confirmation.
     * Filtre les doublons (re-vérifie en base pour tenir compte des imports concurrents),
     * insère les nouvelles transactions et retourne un [ImportResult] avec les compteurs.
     * Enveloppé dans [Result] pour que le ViewModel gère les erreurs proprement.
     */
    suspend fun confirmer(transactions: List<ImportedTransaction>): Result<ImportResult> =
        runCatching {
            val existants = repository.externalIdsExistants()
            val nouvelles = transactions.filter { it.externalId !in existants }
            repository.importerTransactions(nouvelles.map { it.toTransaction() })
            ImportResult(importees = nouvelles.size, ignorees = transactions.size - nouvelles.size)
        }
}

/** Résultat d'un import confirmé. */
data class ImportResult(
    val importees: Int,  // transactions effectivement insérées en base
    val ignorees: Int    // doublons détectés et ignorés
)
