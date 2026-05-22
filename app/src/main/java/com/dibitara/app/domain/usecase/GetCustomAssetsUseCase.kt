package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomAssetsUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    operator fun invoke(): Flow<List<CustomAsset>> = repository.getAllCustomAssets()
}
