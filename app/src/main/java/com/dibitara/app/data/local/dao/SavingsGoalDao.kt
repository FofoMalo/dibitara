package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY targetDateEpochDay ASC")
    fun getAll(): Flow<List<SavingsGoalEntity>>

    /** INSERT, ou REPLACE si l'id existe déjà (édition d'un objectif). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavingsGoalEntity)

    @Delete
    suspend fun delete(entity: SavingsGoalEntity)
}
