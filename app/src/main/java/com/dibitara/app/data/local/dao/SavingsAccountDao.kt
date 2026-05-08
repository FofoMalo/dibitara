package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.SavingsAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsAccountDao {
    @Query("SELECT * FROM savings_accounts ORDER BY currentBalanceCents DESC")
    fun getAll(): Flow<List<SavingsAccountEntity>>

    @Query("SELECT * FROM savings_accounts WHERE childId = :childId")
    fun getByChild(childId: Long): Flow<List<SavingsAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: SavingsAccountEntity): Long

    @Update
    suspend fun update(account: SavingsAccountEntity)

    @Delete
    suspend fun delete(account: SavingsAccountEntity)
}
