package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.TransactionTrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTrashDao {
    @Update suspend fun update(entry: TransactionTrashEntity)

    @Query("SELECT * FROM transaction_trash ORDER BY deletedAtEpochMilli DESC")
    fun getAll(): Flow<List<TransactionTrashEntity>>
    @Query("SELECT * FROM transaction_trash WHERE transactionId = :id")
    suspend fun getById(id: Long): TransactionTrashEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: TransactionTrashEntity)
    @Query("DELETE FROM transaction_trash WHERE transactionId = :id")
    suspend fun delete(id: Long)
}
