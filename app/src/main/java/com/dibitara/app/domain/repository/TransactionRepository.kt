package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrat que la couche data DOIT respecter pour les transactions.
 * La couche domain ne connaît pas Room, SQLite, ou toute autre techno de stockage.
 * C'est le principe d'inversion de dépendance (SOLID).
 */
interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getByMonth(month: Int, year: Int): Flow<List<Transaction>>
    fun getByType(type: TransactionType): Flow<List<Transaction>>
    fun getByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    fun getRecurring(): Flow<List<Transaction>>
    suspend fun hasRecurringOccurrenceThisMonth(recurringId: Long, month: Int, year: Int): Boolean
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
}
