package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne la liste de tous les investissements SCPI enregistrés.
class GetScpiUseCase @Inject constructor(
    private val repository: InvestmentRepository
) {
    operator fun invoke(): Flow<List<ScpiInvestment>> = repository.getAllScpi()
}
