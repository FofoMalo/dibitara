package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class SaveCustomAssetUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(asset: CustomAsset): Result<Long> {
        if (asset.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (asset.totalValueCents <= 0) return Result.failure(IllegalArgumentException("La valeur doit être supérieure à 0"))
        return repository.saveCustomAsset(asset)
    }
}
