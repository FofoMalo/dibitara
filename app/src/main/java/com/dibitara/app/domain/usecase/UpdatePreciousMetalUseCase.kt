package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.PreciousMetalAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class UpdatePreciousMetalUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(asset: PreciousMetalAsset): Result<Unit> {
        if (asset.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (asset.quantityGrams <= 0) return Result.failure(IllegalArgumentException("La quantité doit être supérieure à 0"))
        return runCatching { repository.updatePreciousMetal(asset) }
    }
}
