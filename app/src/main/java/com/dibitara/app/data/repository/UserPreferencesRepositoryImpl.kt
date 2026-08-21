package com.dibitara.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.model.ThemeMode
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implémentation de [UserPreferencesRepository] via DataStore (fichier clé-valeur local).
 * DataStore est injecté par Hilt depuis [com.dibitara.app.di.DataStoreModule].
 */
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    companion object {
        val KEY_SEUIL_CENTS                  = longPreferencesKey("seuil_fonds_cents")
        val KEY_DEVISE                       = stringPreferencesKey("devise_par_defaut")
        val KEY_RAPPORT_MENSUEL              = booleanPreferencesKey("afficher_rapport_mensuel")
        val KEY_AFFICHER_EPARGNE             = booleanPreferencesKey("afficher_epargne")
        val KEY_AFFICHER_INVESTISSEMENTS     = booleanPreferencesKey("afficher_investissements")
        val KEY_TWO_FACTOR_ENABLED           = booleanPreferencesKey("two_factor_enabled")
        val KEY_AFFICHER_PROCHAINS_PAIEMENTS = booleanPreferencesKey("afficher_prochains_paiements")
        // Ordre des cartes : noms d'enum séparés par des virgules, ex. "DETTES,RAPPORT_GRAPHIQUE,..."
        val KEY_DASHBOARD_CARD_ORDER         = stringPreferencesKey("dashboard_card_order")
        val KEY_NOTIFICATIONS_MENSUELLES     = booleanPreferencesKey("notifications_mensuelles")
        val KEY_AFFICHER_RECOMMANDATIONS     = booleanPreferencesKey("afficher_recommandations")
        val KEY_TAUX_EPARGNE_CIBLE           = intPreferencesKey("taux_epargne_cible_pct")
        val KEY_DERNIER_IMPORT               = longPreferencesKey("dernier_import_epoch_milli")
        val KEY_MASQUER_MONTANTS             = booleanPreferencesKey("masquer_montants")
        val KEY_THEME_MODE                   = stringPreferencesKey("theme_mode")
        val KEY_DERNIERE_ALERTE_FONDS        = longPreferencesKey("derniere_alerte_fonds_epoch_day")
        val KEY_DERNIERE_ALERTE_BUDGET       = longPreferencesKey("derniere_alerte_budget_epoch_day")
        val KEY_DERNIERE_ALERTE_DETTES       = longPreferencesKey("derniere_alerte_dettes_epoch_day")
    }

    override fun get(): Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            seuilFondsCents             = prefs[KEY_SEUIL_CENTS] ?: UserPreferences().seuilFondsCents,
            deviseParDefaut             = prefs[KEY_DEVISE]
                ?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
                ?: UserPreferences().deviseParDefaut,
            afficherRapportMensuel      = prefs[KEY_RAPPORT_MENSUEL] ?: false,
            afficherEpargne             = prefs[KEY_AFFICHER_EPARGNE] ?: true,
            afficherInvestissements     = prefs[KEY_AFFICHER_INVESTISSEMENTS] ?: true,
            afficherProchainsPaiements  = prefs[KEY_AFFICHER_PROCHAINS_PAIEMENTS] ?: true,
            twoFactorEnabled            = prefs[KEY_TWO_FACTOR_ENABLED] ?: false,
            dashboardCardOrder          = prefs[KEY_DASHBOARD_CARD_ORDER]
                ?.deserializeDashboardOrder()
                ?: DashboardCard.entries.toList(),
            notificationsMensuelles     = prefs[KEY_NOTIFICATIONS_MENSUELLES] ?: false,
            afficherRecommandations     = prefs[KEY_AFFICHER_RECOMMANDATIONS] ?: false,
            tauxEpargneCiblePct         = prefs[KEY_TAUX_EPARGNE_CIBLE] ?: UserPreferences().tauxEpargneCiblePct,
            derniereImportEpochMilli    = prefs[KEY_DERNIER_IMPORT],
            masquerMontants             = prefs[KEY_MASQUER_MONTANTS] ?: false,
            themeMode                   = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: UserPreferences().themeMode,
            derniereAlerteFondsEpochDay = prefs[KEY_DERNIERE_ALERTE_FONDS],
            derniereAlerteBudgetEpochDay = prefs[KEY_DERNIERE_ALERTE_BUDGET],
            derniereAlerteDettesEpochDay = prefs[KEY_DERNIERE_ALERTE_DETTES]
        )
    }

    override suspend fun updateSeuil(seuilCents: Long) {
        dataStore.edit { it[KEY_SEUIL_CENTS] = seuilCents }
    }

    override suspend fun updateDevise(currency: Currency) {
        dataStore.edit { it[KEY_DEVISE] = currency.name }
    }

    override suspend fun updateAfficherRapport(afficher: Boolean) {
        dataStore.edit { it[KEY_RAPPORT_MENSUEL] = afficher }
    }

    override suspend fun updateAfficherEpargne(afficher: Boolean) {
        dataStore.edit { it[KEY_AFFICHER_EPARGNE] = afficher }
    }

    override suspend fun updateAfficherInvestissements(afficher: Boolean) {
        dataStore.edit { it[KEY_AFFICHER_INVESTISSEMENTS] = afficher }
    }

    override suspend fun updateAfficherProchainsPaiements(afficher: Boolean) {
        dataStore.edit { it[KEY_AFFICHER_PROCHAINS_PAIEMENTS] = afficher }
    }

    override suspend fun updateTwoFactorEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TWO_FACTOR_ENABLED] = enabled }
    }

    override suspend fun updateDashboardCardOrder(order: List<DashboardCard>) {
        dataStore.edit { it[KEY_DASHBOARD_CARD_ORDER] = order.serializeDashboardOrder() }
    }

    override suspend fun updateNotificationsMensuelles(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_MENSUELLES] = enabled }
    }

    override suspend fun updateAfficherRecommandations(afficher: Boolean) {
        dataStore.edit { it[KEY_AFFICHER_RECOMMANDATIONS] = afficher }
    }

    override suspend fun updateTauxEpargneCible(pct: Int) {
        dataStore.edit { it[KEY_TAUX_EPARGNE_CIBLE] = pct }
    }

    override suspend fun updateDerniereImport(epochMilli: Long) {
        dataStore.edit { it[KEY_DERNIER_IMPORT] = epochMilli }
    }

    override suspend fun updateMasquerMontants(masquer: Boolean) {
        dataStore.edit { it[KEY_MASQUER_MONTANTS] = masquer }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun updateDerniereAlerteFonds(epochDay: Long) {
        dataStore.edit { it[KEY_DERNIERE_ALERTE_FONDS] = epochDay }
    }

    override suspend fun updateDerniereAlerteBudget(epochDay: Long) {
        dataStore.edit { it[KEY_DERNIERE_ALERTE_BUDGET] = epochDay }
    }

    override suspend fun updateDerniereAlerteDettes(epochDay: Long) {
        dataStore.edit { it[KEY_DERNIERE_ALERTE_DETTES] = epochDay }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // ─── Sérialisation de l'ordre des cartes ─────────────────────────────────

    private fun List<DashboardCard>.serializeDashboardOrder(): String =
        joinToString(",") { it.name }

    // Désérialise en ignorant les valeurs inconnues (migration future sans crash)
    private fun String.deserializeDashboardOrder(): List<DashboardCard> {
        val parsed = split(",").mapNotNull { name ->
            runCatching { DashboardCard.valueOf(name.trim()) }.getOrNull()
        }
        // Ajouter les nouvelles cartes non encore persistées en fin de liste
        val missing = DashboardCard.entries.filter { it !in parsed }
        return parsed + missing
    }
}
