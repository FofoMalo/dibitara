package com.dibitara.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dibitara.app.domain.model.Currency
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
        val KEY_SEUIL_CENTS           = longPreferencesKey("seuil_fonds_cents")
        val KEY_DEVISE                = stringPreferencesKey("devise_par_defaut")
        val KEY_RAPPORT_MENSUEL       = booleanPreferencesKey("afficher_rapport_mensuel")
        val KEY_AFFICHER_EPARGNE      = booleanPreferencesKey("afficher_epargne")
        val KEY_AFFICHER_INVESTISSEMENTS = booleanPreferencesKey("afficher_investissements")
    }

    override fun get(): Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            seuilFondsCents        = prefs[KEY_SEUIL_CENTS] ?: UserPreferences().seuilFondsCents,
            deviseParDefaut        = prefs[KEY_DEVISE]
                ?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
                ?: UserPreferences().deviseParDefaut,
            afficherRapportMensuel = prefs[KEY_RAPPORT_MENSUEL] ?: false,
            afficherEpargne        = prefs[KEY_AFFICHER_EPARGNE] ?: true,
            afficherInvestissements = prefs[KEY_AFFICHER_INVESTISSEMENTS] ?: true
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
}
