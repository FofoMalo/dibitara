package com.dibitara.app.presentation.common

import androidx.compose.runtime.Composable

// Format générique IBAN : 2 lettres pays + 2 chiffres de contrôle + jusqu'à 30 alphanumériques,
// sans espaces (format des exports CSV bancaires, ex. "FR0616348000016613400006S89").
private val IBAN_REGEX = Regex("""\b[A-Z]{2}\d{2}[A-Z0-9]{10,30}\b""")
private const val IBAN_MASQUE = "••••"

/**
 * Masque les IBAN visibles dans les libellés de virements importés (ex. "Sepa Direct Debit
 * transfer to ... FR0616348000016613400006S89") quand le mode confidentialité est actif.
 * Réutilise [LocalMontantsMasques] plutôt qu'un second réglage séparé - un seul bouton
 * "confidentialité" pour l'utilisateur.
 */
@Composable
fun String.maskIban(): String {
    if (!LocalMontantsMasques.current) return this
    return IBAN_REGEX.replace(this) { IBAN_MASQUE }
}
