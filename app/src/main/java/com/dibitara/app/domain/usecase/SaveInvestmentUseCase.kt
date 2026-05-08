package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

class SaveInvestmentUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend fun realEstate(asset: RealEstateAsset): Result<Long> {
        if (asset.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (asset.currentValueCents <= 0) return Result.failure(IllegalArgumentException("La valeur doit être supérieure à 0"))
        return repository.saveRealEstate(asset)
    }

    suspend fun scpi(scpi: ScpiInvestment): Result<Long> {
        if (scpi.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (scpi.sharesCount <= 0) return Result.failure(IllegalArgumentException("Le nombre de parts doit être supérieur à 0"))
        return repository.saveScpi(scpi)
    }

    suspend fun airbnb(rental: AirbnbRental): Result<Long> {
        if (rental.propertyLabel.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (rental.amountCents <= 0) return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        return repository.saveAirbnbRental(rental)
    }
}
