package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionActionsRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionActionsRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> =
        runCatching { repository.supprimer(transaction.id) }
}
