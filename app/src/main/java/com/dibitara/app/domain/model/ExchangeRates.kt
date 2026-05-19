package com.dibitara.app.domain.model

/**
 * Taux de change par rapport à l'Euro (base EUR).
 * [usdParEur]  : combien de dollars pour 1 €
 * [xofParEur]  : combien de francs CFA pour 1 €
 * [horodatage] : date/heure de la dernière mise à jour (epoch millis)
 */
data class ExchangeRates(
    val usdParEur: Double,
    val xofParEur: Double,
    val horodatage: Long
)
