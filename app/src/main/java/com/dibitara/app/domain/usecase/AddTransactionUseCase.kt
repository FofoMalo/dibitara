package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Valide et insère une transaction.
 * La validation métier (montant > 0, date valide) est ici — jamais dans le ViewModel.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.amountCents <= 0) {
            return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        }
        return runCatching { transactionRepository.insert(transaction) }
    }
}
