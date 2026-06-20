package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.ExchangeRates
import kotlinx.coroutines.flow.Flow

/**
 * Contrat pour récupérer les taux de change.
 * L'implémentation gère le cache - l'appelant ne sait pas d'où viennent les taux.
 */
interface ExchangeRateRepository {
    /** Retourne les taux (cache local si récents, sinon appel réseau). */
    suspend fun getRates(): Result<ExchangeRates>

    /**
     * Émet les taux mis en cache dès qu'ils changent (DataStore).
     * Utilise les taux de secours si le cache est vide.
     * Utilisé pour des calculs réactifs (overview patrimonial, rapport mensuel).
     */
    fun getRatesFlow(): Flow<ExchangeRates>
}
