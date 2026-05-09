package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne les revenus Airbnb filtrés pour une année donnée.
class GetAirbnbRentalsByYearUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    operator fun invoke(year: Int): Flow<List<AirbnbRental>> = repository.getAirbnbRentalsByYear(year)
}
