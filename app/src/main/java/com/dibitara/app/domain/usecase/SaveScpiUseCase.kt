package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Valide et enregistre un investissement SCPI.
class SaveScpiUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(scpi: ScpiInvestment): Result<Long> {
        if (scpi.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (scpi.sharesCount <= 0) return Result.failure(IllegalArgumentException("Le nombre de parts doit être supérieur à 0"))
        return repository.saveScpi(scpi)
    }
}
