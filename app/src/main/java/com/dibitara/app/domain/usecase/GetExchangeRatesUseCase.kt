package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.repository.ExchangeRateRepository
import javax.inject.Inject

/**
 * Retourne les taux de change EUR/USD et EUR/XOF.
 * La logique de cache (durée de validité, appel réseau) est dans le repository.
 */
class GetExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): Result<ExchangeRates> = repository.getRates()
}
