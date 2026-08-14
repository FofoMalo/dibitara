package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.fromImportSource
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ImportRepository
import javax.inject.Inject

/**
 * Insère directement une transaction capturée en direct (notification bancaire confirmée).
 * Contrairement à l'import CSV en vrac, la donnée vient d'une confirmation bancaire déjà
 * fiable : pas d'étape de preview, juste une vérification de doublon par externalId
 * (utile si la même notification est traitée deux fois par le système Android).
 */
class CapturerTransactionLiveUseCase @Inject constructor(
    private val repository: ImportRepository,
    private val bankAccountRepository: BankAccountRepository
) {
    /** Retourne true si la transaction a été insérée, false si c'était un doublon. */
    suspend operator fun invoke(transaction: ImportedTransaction): Boolean {
        val existants = repository.externalIdsExistants()
        if (transaction.externalId in existants) return false

        val bankAccountId = BankProvider.fromImportSource(transaction.importSource)
            ?.let { bankAccountRepository.findByProvider(it) }
            ?.id
        repository.importerTransactions(listOf(transaction.toTransaction(bankAccountId = bankAccountId)))
        return true
    }
}
