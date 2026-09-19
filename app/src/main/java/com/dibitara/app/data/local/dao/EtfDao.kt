package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EtfDao {
    @Query("SELECT * FROM etf_plans") fun observePlans(): Flow<List<EtfPlanEntity>>
    @Query("SELECT * FROM etf_purchases ORDER BY dateEpochDay, id") fun observePurchases(): Flow<List<EtfPurchaseEntity>>
    @Query("SELECT * FROM etf_plans WHERE assetId = :assetId") suspend fun plan(assetId: Long): EtfPlanEntity?
    @Query("SELECT * FROM etf_purchases") suspend fun purchases(): List<EtfPurchaseEntity>
    @Upsert suspend fun savePlan(plan: EtfPlanEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(purchase: EtfPurchaseEntity): Long
    @Update suspend fun update(purchase: EtfPurchaseEntity)
    @Query("DELETE FROM etf_purchases WHERE assetId = :assetId AND id = :purchaseId") suspend fun delete(assetId: Long, purchaseId: Long)
}
