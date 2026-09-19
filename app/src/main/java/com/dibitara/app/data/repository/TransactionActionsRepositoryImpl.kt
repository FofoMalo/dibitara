package com.dibitara.app.data.repository

import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.data.local.entity.TransactionTrashEntity
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.DeletedTransaction
import com.dibitara.app.domain.repository.TransactionActionsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionActionsRepositoryImpl @Inject constructor(private val db: DibitaraDatabase) : TransactionActionsRepository {
    private val gson = Gson()
    override fun corbeille() = db.transactionTrashDao().getAll().map { entries ->
        entries.map { DeletedTransaction(gson.fromJson(it.payload, TransactionEntity::class.java).toDomain(), it.deletedAtEpochMilli) }
    }

    override suspend fun supprimer(id: Long) = db.withTransaction {
        val entity = requireNotNull(db.transactionDao().getById(id)) { "Cette opération n'existe plus." }
        db.transactionTrashDao().insert(TransactionTrashEntity(id, gson.toJson(entity), System.currentTimeMillis()))
        db.transactionDao().delete(entity)
    }

    override suspend fun restaurer(id: Long) = db.withTransaction {
        val entry = requireNotNull(db.transactionTrashDao().getById(id)) { "Cette opération n'est plus dans la corbeille." }
        val entity = gson.fromJson(entry.payload, TransactionEntity::class.java)
        require(db.transactionDao().getById(id) == null) { "Une opération porte déjà cet identifiant ; aucune donnée remplacée." }
        val externalIds = db.transactionDao().getAllExternalIds().toSet()
        require(listOfNotNull(entity.externalId, entity.notificationExternalId).none { it in externalIds }) {
            "Cette opération a déjà été réimportée. Consultez les doublons avant de la restaurer."
        }
        db.transactionDao().insert(entity)
        db.transactionTrashDao().delete(id)
    }

    override suspend fun categoriser(ids: Set<Long>, category: Category): Int = db.withTransaction {
        // Relire dans la transaction évite d'écraser des corrections faites depuis la sélection.
        val entries = ids.map { requireNotNull(db.transactionDao().getById(it)) { "Une opération sélectionnée n'existe plus." } }
        require(entries.all { it.type == TransactionType.EXPENSE.name }) { "Sélectionnez uniquement des dépenses." }
        entries.forEach { db.transactionDao().update(it.copy(category = category.name, subCategory = null, customSubCategoryId = null)) }
        entries.size
    }
}
