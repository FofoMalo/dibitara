package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime un bien immobilier de la base, ainsi que son historique de valorisation.
class DeleteRealEstateUseCase @Inject constructor(
    private val repository: InvestmentRepository,
    private val snapshotRepository: AssetValuationSnapshotRepository
) {
    suspend operator fun invoke(asset: RealEstateAsset) {
        repository.deleteRealEstate(asset)
        snapshotRepository.deleteForAsset(AssetValuationType.REAL_ESTATE, asset.id)
    }
}
