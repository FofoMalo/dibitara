package com.dibitara.app.data.remote.api

import com.dibitara.app.data.remote.dto.FrankfurterResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit pour l'API Frankfurter (taux de change gratuits, sans clé).
 * URL de base : https://api.frankfurter.app
 * Exemple d'appel : GET /latest?from=EUR&to=USD,XOF
 */
interface FrankfurterApi {

    @GET("latest")
    suspend fun getLatest(
        @Query("from") base: String = "EUR",
        @Query("to")   targets: String = "USD,XOF"
    ): FrankfurterResponse
}
