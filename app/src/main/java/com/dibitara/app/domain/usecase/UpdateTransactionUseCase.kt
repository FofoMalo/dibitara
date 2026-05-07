package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Met à jour une transaction existante via son ID.
 * Utilise @Update Room — l'ID ne change pas, contrairement à delete+insert.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amountCents <= 0) {
            return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        }
        return runCatching { repository.update(transaction) }
    }
}
