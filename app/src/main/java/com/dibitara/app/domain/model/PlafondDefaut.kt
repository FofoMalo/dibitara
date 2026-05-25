package com.dibitara.app.domain.model

/**
 * Plafonds légaux par défaut selon le type de compte et la devise.
 *
 * Toutes les valeurs sont en centimes pour rester cohérentes avec le reste du modèle.
 * Source : législation française (Banque de France) et opérateurs Orange Money.
 *
 * Le plafond est strictement informatif : l'utilisateur peut le modifier librement
 * et le dépasser (aucun blocage applicatif). Il sert uniquement à l'indicateur
 * visuel de remplissage dans la carte compte épargne.
 */
object PlafondDefaut {

    // Plafonds légaux EUR (France) en centimes
    private const val LIVRET_A_EUR    = 2_295_000L  // 22 950 €
    private const val LDDS_EUR        = 1_200_000L  // 12 000 €
    private const val PEL_EUR         = 6_120_000L  // 61 200 €
    private const val PEA_EUR         = 15_000_000L // 150 000 €

    // Plafond solde Orange Money (XOF) — valeur maximale commune Sénégal/Côte d'Ivoire
    private const val ORANGE_MONEY_XOF = 200_000_000L // 2 000 000 XOF

    /**
     * Retourne le plafond suggéré pour un type de compte dans une devise donnée.
     * Retourne null si aucun plafond légal connu (ex. Assurance vie, Courtier, PER).
     */
    fun suggerer(type: SavingsType, currency: Currency): Long? = when {
        type == SavingsType.LIVRET_A     && currency == Currency.EUR -> LIVRET_A_EUR
        type == SavingsType.LDDS         && currency == Currency.EUR -> LDDS_EUR
        type == SavingsType.PEL          && currency == Currency.EUR -> PEL_EUR
        type == SavingsType.PEA          && currency == Currency.EUR -> PEA_EUR
        type == SavingsType.ORANGE_MONEY && currency == Currency.XOF -> ORANGE_MONEY_XOF
        else -> null
    }
}
