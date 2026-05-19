package com.dibitara.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Réponse JSON de l'API Frankfurter.
 * Exemple : { "base": "EUR", "date": "2026-05-19", "rates": { "USD": 1.0893, "XOF": 655.96 } }
 */
data class FrankfurterResponse(
    @SerializedName("base")  val base: String,
    @SerializedName("date")  val date: String,
    @SerializedName("rates") val rates: Map<String, Double>
)
