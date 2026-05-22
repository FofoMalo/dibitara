package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.CustomAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAssetDao {
    @Query("SELECT * FROM custom_assets ORDER BY label ASC")
    fun getAll(): Flow<List<CustomAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CustomAssetEntity): Long

    @Update
    suspend fun update(entity: CustomAssetEntity)

    @Delete
    suspend fun delete(entity: CustomAssetEntity)
}
