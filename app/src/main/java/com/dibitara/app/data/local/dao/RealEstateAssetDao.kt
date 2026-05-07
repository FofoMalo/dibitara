package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.RealEstateAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RealEstateAssetDao {
    @Query("SELECT * FROM real_estate_assets ORDER BY currentValueCents DESC")
    fun getAll(): Flow<List<RealEstateAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: RealEstateAssetEntity): Long

    @Update
    suspend fun update(asset: RealEstateAssetEntity)

    @Delete
    suspend fun delete(asset: RealEstateAssetEntity)
}
