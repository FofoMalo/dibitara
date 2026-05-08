package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.ScpiInvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScpiInvestmentDao {
    @Query("SELECT * FROM scpi_investments ORDER BY sharesCount DESC")
    fun getAll(): Flow<List<ScpiInvestmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scpi: ScpiInvestmentEntity): Long

    @Update
    suspend fun update(scpi: ScpiInvestmentEntity)

    @Delete
    suspend fun delete(scpi: ScpiInvestmentEntity)
}
