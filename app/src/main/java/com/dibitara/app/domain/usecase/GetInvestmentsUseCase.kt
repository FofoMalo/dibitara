package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInvestmentsUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    fun realEstate(): Flow<List<RealEstateAsset>> = repository.getAllRealEstate()
    fun scpi(): Flow<List<ScpiInvestment>> = repository.getAllScpi()
    fun airbnb(): Flow<List<AirbnbRental>> = repository.getAllAirbnbRentals()
    fun airbnbByYear(year: Int): Flow<List<AirbnbRental>> = repository.getAirbnbRentalsByYear(year)
}
