package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

data class DeletedTransaction(val transaction: Transaction, val deletedAtEpochMilli: Long)

interface TransactionActionsRepository {
    fun corbeille(): Flow<List<DeletedTransaction>>
    suspend fun supprimer(id: Long)
    suspend fun restaurer(id: Long)
    suspend fun categoriser(ids: Set<Long>, category: Category): Int
}
