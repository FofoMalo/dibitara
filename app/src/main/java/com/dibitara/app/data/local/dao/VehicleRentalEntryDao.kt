package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.VehicleRentalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleRentalEntryDao {
    @Query("SELECT * FROM vehicle_rental_entries ORDER BY dateEpochDay DESC")
    fun getAll(): Flow<List<VehicleRentalEntryEntity>>

    // Filtre par année (365 jours à partir du 1er janvier de l'année)
    @Query("SELECT * FROM vehicle_rental_entries WHERE dateEpochDay >= :fromEpoch AND dateEpochDay <= :toEpoch ORDER BY dateEpochDay DESC")
    fun getByYear(fromEpoch: Long, toEpoch: Long): Flow<List<VehicleRentalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VehicleRentalEntryEntity): Long

    @Update
    suspend fun update(entry: VehicleRentalEntryEntity)

    @Delete
    suspend fun delete(entry: VehicleRentalEntryEntity)
}
