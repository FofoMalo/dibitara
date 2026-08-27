package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import javax.inject.Inject

// Retourne une transaction précise par son id, ou null si elle n'existe pas (ou plus).
class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Transaction? = repository.getById(id)
}
