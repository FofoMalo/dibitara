package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Modifie le nombre de parts ou la valeur unitaire d'une SCPI existante.
class UpdateScpiUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(scpi: ScpiInvestment): Result<Unit> {
        if (scpi.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (scpi.sharesCount <= 0) return Result.failure(IllegalArgumentException("Le nombre de parts doit être supérieur à 0"))
        return runCatching { repository.updateScpi(scpi) }
    }
}
