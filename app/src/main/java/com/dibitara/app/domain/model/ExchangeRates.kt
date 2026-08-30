package com.dibitara.app.domain.model

/**
 * Taux de change par rapport à l'Euro (base EUR).
 * [usdParEur]  : combien de dollars pour 1 €
 * [xofParEur]  : combien de francs CFA pour 1 €
 * [cadParEur]  : combien de dollars canadiens pour 1 €
 * [horodatage] : date/heure de la dernière mise à jour (epoch millis)
 *
 * [cadParEur] a une valeur par défaut pour ne pas casser les appels positionnels à 3
 * arguments déjà écrits (constructions de test notamment) - même rôle que USD_FALLBACK
 * dans ExchangeRateRepositoryImpl, dupliqué ici car ce modèle est pur Kotlin sans
 * dépendance vers la couche data.
 */
data class ExchangeRates(
    val usdParEur: Double,
    val xofParEur: Double,
    val horodatage: Long,
    val cadParEur: Double = 1.47
)
