package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    /** Émet les préférences à chaque modification — Flow actif en temps réel. */
    fun get(): Flow<UserPreferences>
    suspend fun updateSeuil(seuilCents: Long)
    suspend fun updateDevise(currency: Currency)
    suspend fun updateAfficherRapport(afficher: Boolean)
    suspend fun updateAfficherEpargne(afficher: Boolean)
    suspend fun updateAfficherInvestissements(afficher: Boolean)
    suspend fun updateAfficherProchainsPaiements(afficher: Boolean)
    suspend fun updateTwoFactorEnabled(enabled: Boolean)
}
