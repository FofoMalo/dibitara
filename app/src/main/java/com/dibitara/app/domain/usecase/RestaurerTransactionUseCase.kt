package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.TransactionActionsRepository
import javax.inject.Inject

class RestaurerTransactionUseCase @Inject constructor(private val repository: TransactionActionsRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching { repository.restaurer(id) }
}
