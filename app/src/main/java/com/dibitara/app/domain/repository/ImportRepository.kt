package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Transaction
import java.time.LocalDate

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

    /**
     * Cherche une capture live (notification bancaire, [Transaction.importSource] = "bred_notification")
     * correspondant à un mouvement du CSV importé - même montant, date à ±1 jour près.
     * Utilisé pour réconcilier plutôt que dupliquer quand le CSV mensuel rattrape une capture live.
     */
    suspend fun trouverCaptureLiveProche(date: LocalDate, amountCents: Long): Transaction?

    /** Met à jour une transaction existante (réconciliation capture live -> CSV). */
    suspend fun mettreAJour(transaction: Transaction)
}
