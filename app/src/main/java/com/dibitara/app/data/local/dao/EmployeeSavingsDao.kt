package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.EmployeeSavingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeSavingsDao {
    @Query("SELECT * FROM employee_savings ORDER BY type ASC, label ASC")
    fun getAll(): Flow<List<EmployeeSavingsEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EmployeeSavingsEntity): Long

    @Update
    suspend fun update(entity: EmployeeSavingsEntity)

    @Delete
    suspend fun delete(entity: EmployeeSavingsEntity)
}
