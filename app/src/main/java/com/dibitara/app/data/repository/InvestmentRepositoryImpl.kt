package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.AirbnbRentalDao
import com.dibitara.app.data.local.dao.RealEstateAssetDao
import com.dibitara.app.data.local.dao.ScpiInvestmentDao
import com.dibitara.app.data.local.entity.AirbnbRentalEntity
import com.dibitara.app.data.local.entity.RealEstateAssetEntity
import com.dibitara.app.data.local.entity.ScpiInvestmentEntity
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class InvestmentRepositoryImpl @Inject constructor(
    private val realEstateDao: RealEstateAssetDao,
    private val scpiDao: ScpiInvestmentDao,
    private val airbnbDao: AirbnbRentalDao
) : InvestmentRepository {

    override fun getAllRealEstate(): Flow<List<RealEstateAsset>> =
        realEstateDao.getAll().map { it.map { e -> e.toDomain() } }

    override suspend fun saveRealEstate(asset: RealEstateAsset): Result<Long> = runCatching {
        realEstateDao.insert(RealEstateAssetEntity.fromDomain(asset))
    }

    override suspend fun updateRealEstate(asset: RealEstateAsset) {
        realEstateDao.update(RealEstateAssetEntity.fromDomain(asset))
    }

    override suspend fun deleteRealEstate(asset: RealEstateAsset) {
        realEstateDao.delete(RealEstateAssetEntity.fromDomain(asset))
    }

    override fun getAllScpi(): Flow<List<ScpiInvestment>> =
        scpiDao.getAll().map { it.map { e -> e.toDomain() } }

    override suspend fun saveScpi(scpi: ScpiInvestment): Result<Long> = runCatching {
        scpiDao.insert(ScpiInvestmentEntity.fromDomain(scpi))
    }

    override suspend fun updateScpi(scpi: ScpiInvestment) {
        scpiDao.update(ScpiInvestmentEntity.fromDomain(scpi))
    }

    override suspend fun deleteScpi(scpi: ScpiInvestment) {
        scpiDao.delete(ScpiInvestmentEntity.fromDomain(scpi))
    }

    override fun getAllAirbnbRentals(): Flow<List<AirbnbRental>> =
        airbnbDao.getAll().map { it.map { e -> e.toDomain() } }

    override fun getAirbnbRentalsByYear(year: Int): Flow<List<AirbnbRental>> {
        val from = LocalDate.of(year, 1, 1).toEpochDay()
        val to = LocalDate.of(year, 12, 31).toEpochDay()
        return airbnbDao.getByYear(from, to).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun saveAirbnbRental(rental: AirbnbRental): Result<Long> = runCatching {
        airbnbDao.insert(AirbnbRentalEntity.fromDomain(rental))
    }

    override suspend fun updateAirbnbRental(rental: AirbnbRental) {
        airbnbDao.update(AirbnbRentalEntity.fromDomain(rental))
    }

    override suspend fun deleteAirbnbRental(rental: AirbnbRental) {
        airbnbDao.delete(AirbnbRentalEntity.fromDomain(rental))
    }
}
