package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

// Retourne les transactions entre [from] et [to] inclus - délègue le filtre de date à Room.
class GetTransactionsByDateRangeUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        repository.getByDateRange(from, to)
}
