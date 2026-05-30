package com.dibitara.app.presentation.common

import com.dibitara.app.domain.model.Currency
import java.util.Locale

fun Long.toCurrencyDisplay(currency: Currency): String = when (currency) {
    // Le franc CFA ne s'exprime pas en centimes dans l'usage courant.
    // Locale.FRENCH garantit l'espace insécable comme séparateur de milliers (ex : "5 000 FCFA").
    Currency.XOF,
    Currency.XAF -> String.format(Locale.FRENCH, "%,.0f %s", this / 100.0, currency.symbol)
    else         -> "%.2f %s".format(this / 100.0, currency.symbol)
}
