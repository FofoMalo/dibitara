package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Transaction

/**
 * Contrat pour la déduplication et la persistance des transactions importées
 * depuis un fichier CSV. Isolé de [TransactionRepository] pour ne pas mélanger
 * la logique d'import avec le CRUD principal.
 */
interface ImportRepository {

    /** Tous les `externalId` déjà présents en base, toutes sources confondues. */
    suspend fun externalIdsExistants(): Set<String>

    /** Insère les transactions et retourne le nombre effectivement inséré. */
    suspend fun importerTransactions(transactions: List<Transaction>): Int
}
