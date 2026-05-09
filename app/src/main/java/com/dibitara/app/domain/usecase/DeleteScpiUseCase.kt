package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime un investissement SCPI de la base.
class DeleteScpiUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    suspend operator fun invoke(scpi: ScpiInvestment) = repository.deleteScpi(scpi)
}
