package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Modifie la valeur ou le libellé d'un bien immobilier existant.
class UpdateRealEstateUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(asset: RealEstateAsset): Result<Unit> {
        if (asset.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (asset.currentValueCents <= 0) return Result.failure(IllegalArgumentException("La valeur doit être supérieure à 0"))
        return runCatching { repository.updateRealEstate(asset) }
    }
}
