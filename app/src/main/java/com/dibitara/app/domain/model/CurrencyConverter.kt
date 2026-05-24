package com.dibitara.app.domain.model

/**
 * Utilitaire pur (sans DI) pour convertir des montants entre devises.
 *
 * Règles de conversion — tout passe par l'EUR comme devise pivot :
 *   USD → EUR : amountCents / usdParEur
 *   XOF → EUR : amountCents / xofParEur
 *   EUR → USD : amountCents * usdParEur
 *   EUR → XOF : amountCents * xofParEur
 *
 * XOF et XAF sont à parité 1:1 (même ancrage BCE, même taux).
 * Si [from] == [to], les centimes sont retournés tels quels.
 */
object CurrencyConverter {

    fun convertCents(
        amountCents : Long,
        from        : Currency,
        to          : Currency,
        rates       : ExchangeRates
    ): Long {
        if (from == to) return amountCents

        // Normalisation XAF → XOF pour simplifier les cas (parité stricte)
        val fromN = if (from == Currency.XAF) Currency.XOF else from
        val toN   = if (to   == Currency.XAF) Currency.XOF else to
        if (fromN == toN) return amountCents

        // 1. Convertir vers EUR (devise pivot)
        val enEurCents: Long = when (fromN) {
            Currency.EUR -> amountCents
            Currency.USD -> (amountCents / rates.usdParEur).toLong()
            Currency.XOF -> (amountCents / rates.xofParEur).toLong()
            else         -> amountCents
        }

        // 2. Convertir depuis EUR vers la devise cible
        return when (toN) {
            Currency.EUR -> enEurCents
            Currency.USD -> (enEurCents * rates.usdParEur).toLong()
            Currency.XOF -> (enEurCents * rates.xofParEur).toLong()
            else         -> enEurCents
        }
    }
}
