package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne la liste de tous les biens immobiliers enregistrés.
class GetRealEstateUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    operator fun invoke(): Flow<List<RealEstateAsset>> = repository.getAllRealEstate()
}
