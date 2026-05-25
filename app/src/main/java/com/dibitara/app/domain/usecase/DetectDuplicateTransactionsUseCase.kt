package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.DuplicateGroup
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Détecte les transactions en double dans la base locale.
 *
 * Deux transactions sont considérées doublons si elles partagent exactement :
 * - la même date (jour)
 * - le même montant en centimes
 * - la même devise
 * - le même type (EXPENSE / INCOME / INVESTMENT)
 *
 * Le libellé n'est pas utilisé comme critère pour éviter les faux positifs
 * (deux transactions légitimes du même montant le même jour avec un libellé légèrement différent).
 *
 * Retourne uniquement les groupes d'au moins 2 transactions, triés par date décroissante.
 */
class DetectDuplicateTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<DuplicateGroup>> =
        repository.getAll().map { transactions ->
            transactions
                .groupBy { t -> Triple(t.date, t.amountCents, t.currency) to t.type }
                .values
                .filter { it.size >= 2 }
                // Tri par date décroissante pour afficher les doublons récents en premier
                .sortedByDescending { it.first().date }
                .map { groupe -> DuplicateGroup(groupe.sortedBy { it.id }) }
        }
}
