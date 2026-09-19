package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.fromImportSource
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TradeRepublicReconciliation
import com.dibitara.app.domain.repository.BankAccountRepository
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bankAccountRepository: BankAccountRepository
) {
    /**
     * Étape 1 - Preview.
     * Compare les [ImportedTransaction.externalId] avec ceux déjà en base et
     * retourne la liste avec [ImportedTransaction.alreadyImported] positionné.
     * N'insère rien.
     */
    suspend fun verifierDoublons(transactions: List<ImportedTransaction>): List<ImportedTransaction> {
        val existants = repository.externalIdsExistants()
        val rapprochements = preparerRapprochements(transactions.filter { it.externalId !in existants })
        return transactions.distinctBy { it.externalId }.map {
            it.copy(alreadyImported = it.externalId in existants, captureLiveReconnue = it.externalId in rapprochements)
        }
    }

    /** Appariement un-à-un sur le lot complet : ne jamais consommer deux fois une capture. */
    private suspend fun preparerRapprochements(transactions: List<ImportedTransaction>): Map<String, Transaction> {
        val csv = transactions.filter { it.importSource == "trade_republic" }.distinctBy { it.externalId }
        if (csv.isEmpty()) return emptyMap()
        val captures = repository.transactionsTradeRepublic().filter { it.importSource == "trade_republic_notification" }
        val resultats = csv.mapNotNull { ligne ->
            val tx = ligne.toTransaction()
            TradeRepublicReconciliation.verifierUnique(tx, TradeRepublicReconciliation.candidats(tx, captures))
                ?.let { ligne.externalId to it }
        }
        require(resultats.map { it.second.id }.distinct().size == resultats.size) {
            "Plusieurs lignes CSV correspondent à la même capture TradeRepublic. Vérifiez ces opérations avant l’import."
        }
        return resultats.toMap()
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
        runCatching { repository.avecTransaction {
            val existants = repository.externalIdsExistants()
            val nouvelles = transactions.distinctBy { it.externalId }.filter { it.externalId !in existants }
            val rapprochements = preparerRapprochements(nouvelles)

            // Un réimport enrichit aussi les anciennes lignes CSV dont la clé n’était pas conservée.
            if (transactions.any { it.importSource == "trade_republic" && it.externalId in existants }) {
                val anciennes = repository.transactionsTradeRepublic().associateBy { it.externalId }
                transactions.filter { it.importSource == "trade_republic" }.forEach { ligne ->
                    anciennes[ligne.externalId]?.takeIf { it.reconciliationKey == null && ligne.reconciliationKey != null }
                        ?.let { repository.mettreAJour(it.copy(reconciliationKey = ligne.reconciliationKey)) }
                }
            }

            val nouvellesAvecCorrespondance = nouvelles.map { imported ->
                val captureLive = if (imported.importSource == "bred") {
                    repository.trouverCaptureLiveProche(imported.date, imported.amountCents)
                } else rapprochements[imported.externalId]
                imported to captureLive
            }

            val (aReconcilier, aInserer) = nouvellesAvecCorrespondance.partition { it.second != null }

            aReconcilier.forEach { (imported, captureLive) ->
                if (imported.importSource == "trade_republic") {
                    // Garder l’id local et les corrections utilisateur (note, catégorie, sous-catégorie).
                    repository.mettreAJour(captureLive!!.copy(
                        externalId = imported.externalId,
                        notificationExternalId = captureLive.externalId,
                        importSource = "trade_republic",
                        reconciliationKey = imported.reconciliationKey,
                        bankAccountId = captureLive.bankAccountId ?: bankAccountRepository.findByProvider(BankProvider.TRADE_REPUBLIC)?.id
                    ))
                } else {
                    repository.mettreAJour(
                        captureLive!!.copy(
                            note         = imported.note,
                            category     = imported.category,
                            importSource = imported.importSource,
                            externalId   = imported.externalId
                        )
                    )
                }
            }

            repository.importerTransactions(aInserer.map { (imported, _) ->
                val bankAccountId = BankProvider.fromImportSource(imported.importSource)
                    ?.let { bankAccountRepository.findByProvider(it) }
                    ?.id
                imported.toTransaction(bankAccountId = bankAccountId)
            })
            userPreferencesRepository.updateDerniereImport(System.currentTimeMillis())
            ImportResult(importees = aInserer.size, ignorees = transactions.size - aInserer.size - rapprochements.size,
                reconciliees = rapprochements.size)
        } }
}

/** Résultat d'un import confirmé. */
data class ImportResult(
    val importees: Int,  // transactions effectivement insérées en base
    val ignorees: Int,   // doublons ignorés, y compris les rapprochements BRED historiques
    val reconciliees: Int = 0
)
