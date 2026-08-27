package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.VehicleRentalEntry
import kotlinx.coroutines.flow.Flow

interface InvestmentRepository {
    fun getAllRealEstate(): Flow<List<RealEstateAsset>>
    suspend fun saveRealEstate(asset: RealEstateAsset): Result<Long>
    suspend fun updateRealEstate(asset: RealEstateAsset)
    suspend fun deleteRealEstate(asset: RealEstateAsset)

    fun getAllScpi(): Flow<List<ScpiInvestment>>
    suspend fun saveScpi(scpi: ScpiInvestment): Result<Long>
    suspend fun updateScpi(scpi: ScpiInvestment)
    suspend fun deleteScpi(scpi: ScpiInvestment)

    fun getAllAirbnbRentals(): Flow<List<AirbnbRental>>
    fun getAirbnbRentalsByYear(year: Int): Flow<List<AirbnbRental>>
    suspend fun saveAirbnbRental(rental: AirbnbRental): Result<Long>
    suspend fun updateAirbnbRental(rental: AirbnbRental)
    suspend fun deleteAirbnbRental(rental: AirbnbRental)

    fun getAllVehicleRentalEntries(): Flow<List<VehicleRentalEntry>>
    fun getVehicleRentalEntriesByYear(year: Int): Flow<List<VehicleRentalEntry>>
    suspend fun saveVehicleRentalEntry(entry: VehicleRentalEntry): Result<Long>
    suspend fun updateVehicleRentalEntry(entry: VehicleRentalEntry)
    suspend fun deleteVehicleRentalEntry(entry: VehicleRentalEntry)
}
