package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.ImportRepository
import java.time.LocalDate
import javax.inject.Inject
import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase

/**
 * Implémentation de [ImportRepository] - s'appuie sur le [TransactionDao] existant.
 * Pas de nouveau DAO nécessaire : les transactions importées vivent dans la même table.
 */
class ImportRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val database: DibitaraDatabase
) : ImportRepository {

    override suspend fun <T> avecTransaction(action: suspend () -> T): T = database.withTransaction { action() }

    override suspend fun transactionsTradeRepublic(): List<Transaction> =
        dao.getTradeRepublicTransactions().map { it.toDomain() }

    override suspend fun externalIdsExistants(): Set<String> =
        dao.getAllExternalIds().toSet()

    override suspend fun importerTransactions(transactions: List<Transaction>): Int {
        transactions.forEach { tx ->
            val rule = if(tx.type==com.dibitara.app.domain.model.TransactionType.EXPENSE && !tx.categoryConfirmed)
                database.categorizationRuleDao().getByNote(tx.note.trim().lowercase()) else null
            val value = if(rule==null) tx else {
                val definitions=database.categoryDefinitionDao().all()
                val key=rule.customSubCategoryId?.let { "u:$it" } ?: rule.subCategory?.let { "s:$it" } ?: "c:${rule.category}"
                if(definitions.any { it.archived && (it.key==key || it.key=="c:${rule.category}") })tx
                else tx.copy(category=rule.toDomain().category,subCategory=rule.toDomain().subCategory,customSubCategoryId=rule.customSubCategoryId,categoryConfirmed=true)
            }
            dao.insert(TransactionEntity.fromDomain(value))
        }
        return transactions.size
    }

    override suspend fun trouverCaptureLiveProche(date: LocalDate, amountCents: Long): Transaction? =
        dao.findByAmountDateRangeAndSource(
            amountCents  = amountCents,
            fromEpoch    = date.minusDays(1).toEpochDay(),
            toEpoch      = date.plusDays(1).toEpochDay(),
            importSource = "bred_notification"
        )?.toDomain()

    override suspend fun mettreAJour(transaction: Transaction) {
        dao.update(TransactionEntity.fromDomain(transaction))
    }
}
