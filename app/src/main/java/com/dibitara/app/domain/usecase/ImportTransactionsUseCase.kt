package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

/**
 * Gère l'import en deux phases :
 *  1. [verifierDoublons] - preview sans écriture : enrichit la liste avec [ImportedTransaction.alreadyImported]
 *  2. [confirmer]        - écriture réelle : insère uniquement les transactions nouvelles
 *
 * La séparation en deux étapes permet d'afficher un écran de preview à l'utilisateur
 * avant de toucher la base de données.
 *
 * Point d'entrée commun aux trois flux d'import (BRED CSV, BRED PDF, TradeRepublic) :
 * [confirmer] enregistre donc ici la date du dernier import, quelle que soit la source.
 */
class ImportTransactionsUseCase @Inject constructor(
    private val repository: ImportRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Étape 1 - Preview.
     * Compare les [ImportedTransaction.externalId] avec ceux déjà en base et
     * retourne la liste avec [ImportedTransaction.alreadyImported] positionné.
     * N'insère rien.
     */
    suspend fun verifierDoublons(transactions: List<ImportedTransaction>): List<ImportedTransaction> {
        val existants = repository.externalIdsExistants()
        return transactions.map { it.copy(alreadyImported = it.externalId in existants) }
    }

    /**
     * Étape 2 - Confirmation.
     * Filtre les doublons (re-vérifie en base pour tenir compte des imports concurrents),
     * insère les nouvelles transactions et retourne un [ImportResult] avec les compteurs.
     * Enveloppé dans [Result] pour que le ViewModel gère les erreurs proprement.
     *
     * Cas particulier BRED : une ligne CSV peut correspondre à une transaction déjà capturée
     * en direct via notification (voir [com.dibitara.app.data.notification.BredNotificationListenerService]).
     * Le libellé n'étant jamais comparable entre les deux sources, la correspondance se fait par
     * montant + date (±1 jour). Si trouvée, la capture live est mise à jour avec le libellé du CSV
     * (qui devient la version canonique) plutôt que d'être dupliquée.
     */
    suspend fun confirmer(transactions: List<ImportedTransaction>): Result<ImportResult> =
        runCatching {
            val existants = repository.externalIdsExistants()
            val nouvelles = transactions.filter { it.externalId !in existants }

            val nouvellesAvecCorrespondance = nouvelles.map { imported ->
                val captureLive = if (imported.importSource == "bred") {
                    repository.trouverCaptureLiveProche(imported.date, imported.amountCents)
                } else null
                imported to captureLive
            }

            val (aReconcilier, aInserer) = nouvellesAvecCorrespondance.partition { it.second != null }

            aReconcilier.forEach { (imported, captureLive) ->
                repository.mettreAJour(
                    captureLive!!.copy(
                        note         = imported.note,
                        category     = imported.category,
                        importSource = imported.importSource,
                        externalId   = imported.externalId
                    )
                )
            }

            repository.importerTransactions(aInserer.map { it.first.toTransaction() })
            userPreferencesRepository.updateDerniereImport(System.currentTimeMillis())
            ImportResult(importees = aInserer.size, ignorees = transactions.size - aInserer.size)
        }
}

/** Résultat d'un import confirmé. */
data class ImportResult(
    val importees: Int,  // transactions effectivement insérées en base
    val ignorees: Int    // doublons ignorés, y compris les captures live réconciliées (mises à jour, pas insérées)
)
