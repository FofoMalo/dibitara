package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne les entrées du véhicule locatif filtrées pour une année donnée.
class GetVehicleRentalEntriesByYearUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    operator fun invoke(year: Int): Flow<List<VehicleRentalEntry>> = repository.getVehicleRentalEntriesByYear(year)
}
