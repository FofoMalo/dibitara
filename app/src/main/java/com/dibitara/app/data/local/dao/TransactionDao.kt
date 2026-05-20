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

    // Retourne tous les modèles récurrents (templates créés par l'utilisateur)
    @Query("SELECT * FROM transactions WHERE isRecurring = 1 ORDER BY dateEpochDay DESC")
    fun getRecurring(): Flow<List<TransactionEntity>>

    // Compte les occurrences déjà générées dans une plage de dates pour un modèle donné
    @Query("SELECT COUNT(*) FROM transactions WHERE sourceRecurringId = :recurringId AND dateEpochDay >= :fromEpoch AND dateEpochDay <= :toEpoch")
    suspend fun countBySourceAndRange(recurringId: Long, fromEpoch: Long, toEpoch: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
