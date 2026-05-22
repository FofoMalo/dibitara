package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.PreciousMetalAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class DeletePreciousMetalUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(asset: PreciousMetalAsset) = repository.deletePreciousMetal(asset)
}
