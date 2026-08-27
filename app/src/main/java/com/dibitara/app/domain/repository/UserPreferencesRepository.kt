package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.model.ThemeMode
import com.dibitara.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    /** Émet les préférences à chaque modification - Flow actif en temps réel. */
    fun get(): Flow<UserPreferences>
    suspend fun updateSeuil(seuilCents: Long)
    /** Met à jour le seuil de reste à vivre mensuel minimum du Scénario logement. */
    suspend fun updateSeuilResteAVivreLogement(seuilCents: Long)
    suspend fun updateDevise(currency: Currency)
    suspend fun updateAfficherRapport(afficher: Boolean)
    suspend fun updateAfficherEpargne(afficher: Boolean)
    suspend fun updateAfficherInvestissements(afficher: Boolean)
    suspend fun updateAfficherProchainsPaiements(afficher: Boolean)
    suspend fun updateTwoFactorEnabled(enabled: Boolean)
    suspend fun updateDashboardCardOrder(order: List<DashboardCard>)
    suspend fun updateNotificationsMensuelles(enabled: Boolean)
    suspend fun updateAfficherRecommandations(afficher: Boolean)
    suspend fun updateTauxEpargneCible(pct: Int)
    /** Enregistre la date + heure (epoch milli) du dernier import CSV/PDF réussi. */
    suspend fun updateDerniereImport(epochMilli: Long)
    suspend fun updateMasquerMontants(masquer: Boolean)
    suspend fun updateThemeMode(mode: ThemeMode)
    /** Enregistre le jour (epoch day) de la dernière alerte "liquidités insuffisantes" envoyée. */
    suspend fun updateDerniereAlerteFonds(epochDay: Long)
    /** Enregistre le jour (epoch day) de la dernière alerte "budget dépassé" envoyée. */
    suspend fun updateDerniereAlerteBudget(epochDay: Long)
    /** Enregistre le jour (epoch day) du dernier envoi des rappels d'échéance dette. */
    suspend fun updateDerniereAlerteDettes(epochDay: Long)
    /** Efface toutes les préférences stockées dans DataStore. */
    suspend fun clearAll()
}
