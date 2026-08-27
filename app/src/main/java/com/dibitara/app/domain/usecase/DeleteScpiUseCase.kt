package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import javax.inject.Inject

// Supprime un investissement SCPI de la base, ainsi que son historique de valorisation.
class DeleteScpiUseCase @Inject constructor(
    private val repository: InvestmentRepository,
    private val snapshotRepository: AssetValuationSnapshotRepository
) {
    suspend operator fun invoke(scpi: ScpiInvestment) {
        repository.deleteScpi(scpi)
        snapshotRepository.deleteForAsset(AssetValuationType.SCPI, scpi.id)
    }
}
