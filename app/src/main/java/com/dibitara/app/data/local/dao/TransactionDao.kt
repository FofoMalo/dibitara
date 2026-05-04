package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    /**
     * Filtre par mois/année en utilisant l'epoch day.
     * On calcule les bornes côté Kotlin pour rester en Long dans Room.
     */
    @Query("SELECT * FROM transactions WHERE dateEpochDay >= :fromEpoch AND dateEpochDay <= :toEpoch ORDER BY dateEpochDay DESC")
    fun getByDateRange(fromEpoch: Long, toEpoch: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateEpochDay DESC")
    fun getByType(type: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
