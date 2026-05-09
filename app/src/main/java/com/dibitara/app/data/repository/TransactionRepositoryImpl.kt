package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implémentation concrète du contrat TransactionRepository.
 * C'est ici que la couche domain "touche" Room — nulle part ailleurs.
 * Hilt injecte cette classe partout où TransactionRepository est demandé.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val from = LocalDate.of(year, month, 1).toEpochDay()
        val to   = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toEpochDay()
        return dao.getByDateRange(from, to).map { list -> list.map { it.toDomain() } }
    }

    override fun getByType(type: TransactionType): Flow<List<Transaction>> =
        dao.getByType(type.name).map { list -> list.map { it.toDomain() } }

    override fun getByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        dao.getByDateRange(from.toEpochDay(), to.toEpochDay())
            .map { list -> list.map { it.toDomain() } }

    override fun getRecurring(): Flow<List<Transaction>> =
        dao.getRecurring().map { list -> list.map { it.toDomain() } }

    override suspend fun hasRecurringOccurrenceThisMonth(recurringId: Long, month: Int, year: Int): Boolean {
        val from = LocalDate.of(year, month, 1).toEpochDay()
        val to   = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toEpochDay()
        return dao.countBySourceAndMonth(recurringId, from, to) > 0
    }

    override suspend fun insert(transaction: Transaction): Long =
        dao.insert(TransactionEntity.fromDomain(transaction))

    override suspend fun update(transaction: Transaction) =
        dao.update(TransactionEntity.fromDomain(transaction))

    override suspend fun delete(transaction: Transaction) =
        dao.delete(TransactionEntity.fromDomain(transaction))
}
