package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Transaction

/**
 * Contrat pour la déduplication et la persistance des transactions importées.
 * Séparé de [TransactionRepository] pour isoler la logique d'import
 * et éviter de polluer le repository principal avec des méthodes métier spécifiques.
 */
interface ImportRepository {
    /** Retourne l'ensemble des externalId déjà présents en base (toutes sources confondues). */
    suspend fun externalIdsExistants(): Set<String>

    /** Insère la liste de transactions et retourne le nombre effectivement inséré. */
    suspend fun importerTransactions(transactions: List<Transaction>): Int
}
