package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne toutes les entrées (revenus et charges) du véhicule locatif, toutes années confondues.
class GetVehicleRentalEntriesUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    operator fun invoke(): Flow<List<VehicleRentalEntry>> = repository.getAllVehicleRentalEntries()
}
