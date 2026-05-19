package com.dibitara.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.dibitara.app.data.remote.api.FrankfurterApi
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Récupère les taux de change depuis l'API Frankfurter avec un cache DataStore.
 * Stratégie : si les taux ont moins d'une heure, on retourne le cache.
 *             Sinon on appelle le réseau et on met à jour le cache.
 */
class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
    private val dataStore: DataStore<Preferences>
) : ExchangeRateRepository {

    companion object {
        val KEY_USD  = doublePreferencesKey("exchange_usd_par_eur")
        val KEY_XOF  = doublePreferencesKey("exchange_xof_par_eur")
        val KEY_TIME = longPreferencesKey("exchange_timestamp")

        // Durée de validité du cache : 1 heure
        const val CACHE_DUREE_MS = 60 * 60 * 1_000L

        // Taux de secours si aucun cache et réseau indisponible
        const val USD_FALLBACK = 1.09
        const val XOF_FALLBACK = 655.96
    }

    override suspend fun getRates(): Result<ExchangeRates> {
        val prefs = dataStore.data.first()
        val timestamp = prefs[KEY_TIME] ?: 0L
        val maintenant = System.currentTimeMillis()

        // Retourne le cache s'il est encore frais
        if (maintenant - timestamp < CACHE_DUREE_MS) {
            val usd = prefs[KEY_USD] ?: USD_FALLBACK
            val xof = prefs[KEY_XOF] ?: XOF_FALLBACK
            return Result.success(ExchangeRates(usd, xof, timestamp))
        }

        // Appel réseau
        return try {
            val response = api.getLatest()
            val usd = response.rates["USD"] ?: USD_FALLBACK
            val xof = response.rates["XOF"] ?: XOF_FALLBACK

            // Sauvegarde en cache
            dataStore.edit { p ->
                p[KEY_USD]  = usd
                p[KEY_XOF]  = xof
                p[KEY_TIME] = maintenant
            }

            Result.success(ExchangeRates(usd, xof, maintenant))
        } catch (e: Exception) {
            // Si le réseau échoue mais qu'on a un vieux cache, on le retourne quand même
            val usd = prefs[KEY_USD] ?: USD_FALLBACK
            val xof = prefs[KEY_XOF] ?: XOF_FALLBACK
            if (prefs[KEY_USD] != null) {
                Result.success(ExchangeRates(usd, xof, timestamp))
            } else {
                Result.failure(e)
            }
        }
    }
}
