package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Valide et enregistre une entrée (revenu ou charge) du véhicule locatif.
class SaveVehicleRentalEntryUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(entry: VehicleRentalEntry): Result<Long> {
        if (entry.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (entry.amountCents <= 0) return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        return repository.saveVehicleRentalEntry(entry)
    }
}
