package com.dibitara.app.domain.model

import kotlin.math.roundToLong

/**
 * Utilitaire pur (sans DI) pour convertir des montants entre devises.
 *
 * Règles de conversion - tout passe par l'EUR comme devise pivot :
 *   USD → EUR : amountCents / usdParEur
 *   XOF → EUR : amountCents / xofParEur
 *   CAD → EUR : amountCents / cadParEur
 *   EUR → USD : amountCents * usdParEur
 *   EUR → XOF : amountCents * xofParEur
 *   EUR → CAD : amountCents * cadParEur
 *
 * XOF et XAF sont à parité 1:1 (même ancrage BCE, même taux).
 * Si [from] == [to], les centimes sont retournés tels quels.
 *
 * ATTENTION : les `when` ci-dessous ont un `else` qui NE CONVERTIT PAS (retourne le
 * montant tel quel). Toute nouvelle devise ajoutée à [Currency] doit être traitée
 * explicitement ici, sinon elle est silencieusement comptée comme si elle valait déjà
 * des EUR partout où le patrimoine est agrégé - pas d'erreur de compilation pour le
 * rappeler.
 */
object CurrencyConverter {

    // Normalisation XAF → XOF pour simplifier les cas (parité stricte)
    private fun normalise(c: Currency) = if (c == Currency.XAF) Currency.XOF else c

    /** True si [a] et [b] désignent la même devise, en tenant compte de la parité XOF/XAF. */
    fun isSameCurrency(a: Currency, b: Currency): Boolean = normalise(a) == normalise(b)

    fun convertCents(
        amountCents : Long,
        from        : Currency,
        to          : Currency,
        rates       : ExchangeRates
    ): Long {
        if (from == to) return amountCents

        val fromN = normalise(from)
        val toN   = normalise(to)
        if (fromN == toN) return amountCents

        // 1. Convertir vers EUR (devise pivot)
        val enEurCents: Long = when (fromN) {
            Currency.EUR -> amountCents
            Currency.USD -> (amountCents / rates.usdParEur).roundToLong()
            Currency.XOF -> (amountCents / rates.xofParEur).roundToLong()
            Currency.CAD -> (amountCents / rates.cadParEur).roundToLong()
            else         -> amountCents
        }

        // 2. Convertir depuis EUR vers la devise cible
        return when (toN) {
            Currency.EUR -> enEurCents
            Currency.USD -> (enEurCents * rates.usdParEur).roundToLong()
            Currency.XOF -> (enEurCents * rates.xofParEur).roundToLong()
            Currency.CAD -> (enEurCents * rates.cadParEur).roundToLong()
            else         -> enEurCents
        }
    }
}
