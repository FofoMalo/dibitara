package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.AirbnbRentalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AirbnbRentalDao {
    @Query("SELECT * FROM airbnb_rentals ORDER BY dateEpochDay DESC")
    fun getAll(): Flow<List<AirbnbRentalEntity>>

    // Filtre par année (365 jours à partir du 1er janvier de l'année)
    @Query("SELECT * FROM airbnb_rentals WHERE dateEpochDay >= :fromEpoch AND dateEpochDay <= :toEpoch ORDER BY dateEpochDay DESC")
    fun getByYear(fromEpoch: Long, toEpoch: Long): Flow<List<AirbnbRentalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rental: AirbnbRentalEntity): Long

    @Update
    suspend fun update(rental: AirbnbRentalEntity)

    @Delete
    suspend fun delete(rental: AirbnbRentalEntity)
}
