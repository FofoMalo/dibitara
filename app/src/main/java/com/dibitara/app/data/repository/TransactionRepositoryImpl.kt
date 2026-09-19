package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implémentation concrète du contrat TransactionRepository.
 * C'est ici que la couche domain "touche" Room - nulle part ailleurs.
 * Hilt injecte cette classe partout où TransactionRepository est demandé.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val catalog: com.dibitara.app.domain.repository.CategoryCatalogRepository
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        dao.getAll().combine(catalog.observe()) { list, c -> list.map { it.toDomain().let { t -> t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) } } }

    override suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toDomain()?.let { t -> val c=catalog.observe().first(); t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) }

    override fun getByMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val from = LocalDate.of(year, month, 1).toEpochDay()
        val to   = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toEpochDay()
        return dao.getByDateRange(from, to).combine(catalog.observe()) { list, c -> list.map { it.toDomain().let { t -> t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) } } }
    }

    override fun getByType(type: TransactionType): Flow<List<Transaction>> =
        dao.getByType(type.name).combine(catalog.observe()) { list, c -> list.map { it.toDomain().let { t -> t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) } } }

    override fun getByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        dao.getByDateRange(from.toEpochDay(), to.toEpochDay())
            .combine(catalog.observe()) { list, c -> list.map { it.toDomain().let { t -> t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) } } }

    override fun getRecurring(): Flow<List<Transaction>> =
        dao.getRecurring().combine(catalog.observe()) { list, c -> list.map { it.toDomain().let { t -> t.copy(category = c.category(t.category),categoryPath=c.label(com.dibitara.app.domain.model.CategoryChoice(t.category,t.subCategory,t.customSubCategoryId))) } } }

    override suspend fun hasRecurringOccurrenceInRange(recurringId: Long, from: LocalDate, to: LocalDate): Boolean =
        dao.countBySourceAndRange(recurringId, from.toEpochDay(), to.toEpochDay()) > 0

    override suspend fun insert(transaction: Transaction): Long =
        dao.insert(TransactionEntity.fromDomain(transaction))

    override suspend fun update(transaction: Transaction) =
        dao.update(TransactionEntity.fromDomain(transaction))

    override suspend fun delete(transaction: Transaction) =
        dao.delete(TransactionEntity.fromDomain(transaction))
}
