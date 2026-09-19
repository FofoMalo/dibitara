package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Copie intégrale de l'opération : les identifiants d'import sont conservés pour la restauration. */
@Entity(tableName = "transaction_trash")
data class TransactionTrashEntity(
    @PrimaryKey val transactionId: Long,
    val payload: String,
    val deletedAtEpochMilli: Long
)
