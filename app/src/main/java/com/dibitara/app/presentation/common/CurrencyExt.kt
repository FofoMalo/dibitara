package com.dibitara.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.dibitara.app.domain.model.Currency
import java.util.Locale

/**
 * Masquage global des montants à l'écran (confidentialité), fourni à la racine de l'app
 * dans MainActivity.setContent depuis la préférence persistée [com.dibitara.app.domain.model.UserPreferences.masquerMontants].
 */
val LocalMontantsMasques = compositionLocalOf { false }

private const val MONTANT_MASQUE = "••••"

/**
 * Formatage brut, sans masquage - utilisable hors composition (ex. message de Snackbar
 * construit dans un `LaunchedEffect`, où [LocalMontantsMasques] n'est pas accessible).
 */
fun Long.formatCurrency(currency: Currency): String = when (currency) {
    // Le franc CFA ne s'exprime pas en centimes dans l'usage courant.
    // Locale.FRENCH garantit l'espace insécable comme séparateur de milliers (ex : "5 000 FCFA").
    Currency.XOF,
    Currency.XAF -> String.format(Locale.FRENCH, "%,.0f %s", this / 100.0, currency.symbol)
    else         -> "%.2f %s".format(this / 100.0, currency.symbol)
}

/**
 * [Composable] plutôt que fonction pure : lit [LocalMontantsMasques] pour masquer le montant
 * partout où il est affiché, sans avoir à modifier chacun des appels existants (une centaine,
 * répartis sur une quinzaine d'écrans).
 */
@Composable
fun Long.toCurrencyDisplay(currency: Currency): String {
    if (LocalMontantsMasques.current) return MONTANT_MASQUE
    return formatCurrency(currency)
}
