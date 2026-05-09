package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Valide et enregistre un bien immobilier.
class SaveRealEstateUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(asset: RealEstateAsset): Result<Long> {
        if (asset.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (asset.currentValueCents <= 0) return Result.failure(IllegalArgumentException("La valeur doit être supérieure à 0"))
        return repository.saveRealEstate(asset)
    }
}
