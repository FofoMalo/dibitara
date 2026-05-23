package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.ImportRepository
import javax.inject.Inject

/**
 * Implémentation de [ImportRepository] — s'appuie sur le [TransactionDao] existant.
 * Pas de nouveau DAO nécessaire : les transactions importées vivent dans la même table.
 */
class ImportRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : ImportRepository {

    override suspend fun externalIdsExistants(): Set<String> =
        dao.getAllExternalIds().toSet()

    override suspend fun importerTransactions(transactions: List<Transaction>): Int {
        transactions.forEach { dao.insert(TransactionEntity.fromDomain(it)) }
        return transactions.size
    }
}
