package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Modifie le montant ou le libellé d'un revenu Airbnb existant.
class UpdateAirbnbRentalUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(rental: AirbnbRental): Result<Unit> {
        if (rental.propertyLabel.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (rental.amountCents <= 0) return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        return runCatching { repository.updateAirbnbRental(rental) }
    }
}
