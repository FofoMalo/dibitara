package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne toutes les transactions sans filtre de date — utilisé par la recherche.
class GetAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAll()
}
