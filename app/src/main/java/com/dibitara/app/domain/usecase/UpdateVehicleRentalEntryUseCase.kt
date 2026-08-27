package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Modifie une entrée (revenu ou charge) existante du véhicule locatif.
class UpdateVehicleRentalEntryUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(entry: VehicleRentalEntry): Result<Unit> {
        if (entry.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (entry.amountCents <= 0) return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        return runCatching { repository.updateVehicleRentalEntry(entry) }
    }
}
