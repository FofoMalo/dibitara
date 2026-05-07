package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

class DeleteInvestmentUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend fun realEstate(asset: RealEstateAsset) = repository.deleteRealEstate(asset)
    suspend fun scpi(scpi: ScpiInvestment) = repository.deleteScpi(scpi)
    suspend fun airbnb(rental: AirbnbRental) = repository.deleteAirbnbRental(rental)
}
