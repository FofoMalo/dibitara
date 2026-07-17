package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime une entrée du véhicule locatif.
class DeleteVehicleRentalEntryUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(entry: VehicleRentalEntry) = repository.deleteVehicleRentalEntry(entry)
}
