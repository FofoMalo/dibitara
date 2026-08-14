package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    @Query("SELECT * FROM bank_accounts ORDER BY label ASC")
    fun getAll(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE provider = :provider LIMIT 1")
    suspend fun findByProvider(provider: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BankAccountEntity): Long

    @Delete
    suspend fun delete(entity: BankAccountEntity)
}
