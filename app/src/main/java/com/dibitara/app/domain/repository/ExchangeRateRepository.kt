package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.ExchangeRates

/**
 * Contrat pour récupérer les taux de change.
 * L'implémentation gère le cache — l'appelant ne sait pas d'où viennent les taux.
 */
interface ExchangeRateRepository {
    /** Retourne les taux (cache local si récents, sinon appel réseau). */
    suspend fun getRates(): Result<ExchangeRates>
}
