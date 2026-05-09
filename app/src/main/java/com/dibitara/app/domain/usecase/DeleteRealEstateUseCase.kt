package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime un bien immobilier de la base.
class DeleteRealEstateUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(asset: RealEstateAsset) = repository.deleteRealEstate(asset)
}
