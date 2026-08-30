package com.dibitara.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.dibitara.app.data.remote.api.FrankfurterApi
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        val KEY_CAD  = doublePreferencesKey("exchange_cad_par_eur")
        val KEY_TIME = longPreferencesKey("exchange_timestamp")

        // Durée de validité du cache : 1 heure
        const val CACHE_DUREE_MS = 60 * 60 * 1_000L

        // Taux de secours si réseau indisponible
        const val USD_FALLBACK = 1.09
        const val CAD_FALLBACK = 1.47

        // XOF (et XAF) sont indexés sur l'euro à parité fixe depuis 1999 (traité de Maastricht).
        // Frankfurter ne les expose pas - inutile d'appeler le réseau pour ces devises.
        const val XOF_TAUX_FIXE = 655.957
    }

    override suspend fun getRates(): Result<ExchangeRates> {
        val prefs = dataStore.data.first()
        val timestamp = prefs[KEY_TIME] ?: 0L
        val maintenant = System.currentTimeMillis()

        // Retourne le cache s'il est encore frais (USD/CAD uniquement - XOF est une constante)
        if (maintenant - timestamp < CACHE_DUREE_MS) {
            val usd = prefs[KEY_USD] ?: USD_FALLBACK
            val cad = prefs[KEY_CAD] ?: CAD_FALLBACK
            return Result.success(ExchangeRates(usd, XOF_TAUX_FIXE, timestamp, cad))
        }

        // Appel réseau pour USD/CAD uniquement
        return try {
            val response = api.getLatest()
            val usd = response.rates["USD"] ?: USD_FALLBACK
            val cad = response.rates["CAD"] ?: CAD_FALLBACK

            dataStore.edit { p ->
                p[KEY_USD]  = usd
                p[KEY_CAD]  = cad
                p[KEY_TIME] = maintenant
            }

            Result.success(ExchangeRates(usd, XOF_TAUX_FIXE, maintenant, cad))
        } catch (e: Exception) {
            val usd = prefs[KEY_USD] ?: USD_FALLBACK
            val cad = prefs[KEY_CAD] ?: CAD_FALLBACK
            if (prefs[KEY_USD] != null) {
                Result.success(ExchangeRates(usd, XOF_TAUX_FIXE, timestamp, cad))
            } else {
                Result.failure(e)
            }
        }
    }

    override fun getRatesFlow(): Flow<ExchangeRates> = dataStore.data.map { prefs ->
        ExchangeRates(
            usdParEur  = prefs[KEY_USD] ?: USD_FALLBACK,
            xofParEur  = XOF_TAUX_FIXE,
            horodatage = prefs[KEY_TIME] ?: 0L,
            cadParEur  = prefs[KEY_CAD] ?: CAD_FALLBACK
        )
    }
}
