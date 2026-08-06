package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.repository.ImportRepository
import javax.inject.Inject

/**
 * Insère directement une transaction capturée en direct (notification bancaire confirmée).
 * Contrairement à l'import CSV en vrac, la donnée vient d'une confirmation bancaire déjà
 * fiable : pas d'étape de preview, juste une vérification de doublon par externalId
 * (utile si la même notification est traitée deux fois par le système Android).
 */
class CapturerTransactionLiveUseCase @Inject constructor(
    private val repository: ImportRepository
) {
    /** Retourne true si la transaction a été insérée, false si c'était un doublon. */
    suspend operator fun invoke(transaction: ImportedTransaction): Boolean {
        val existants = repository.externalIdsExistants()
        if (transaction.externalId in existants) return false
        repository.importerTransactions(listOf(transaction.toTransaction()))
        return true
    }
}
