package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime un revenu Airbnb de la base.
class DeleteAirbnbRentalUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(rental: AirbnbRental) = repository.deleteAirbnbRental(rental)
}
