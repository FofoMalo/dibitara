package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.PreciousMetalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreciousMetalDao {
    @Query("SELECT * FROM precious_metals ORDER BY metalType ASC, label ASC")
    fun getAll(): Flow<List<PreciousMetalEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PreciousMetalEntity): Long

    @Update
    suspend fun update(entity: PreciousMetalEntity)

    @Delete
    suspend fun delete(entity: PreciousMetalEntity)
}
