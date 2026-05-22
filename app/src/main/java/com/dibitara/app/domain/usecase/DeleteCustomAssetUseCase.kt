package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class DeleteCustomAssetUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(asset: CustomAsset) = repository.deleteCustomAsset(asset)
}
