package com.dibitara.app.domain.model

import java.time.LocalDate

data class SavingsAccount(
    val id: Long = 0,
    val type: SavingsType,
    val label: String,
    val currentBalanceCents: Long,
    val monthlyContributionCents: Long,
    val currency: Currency,
    val childId: Long? = null,
    val updatedAt: LocalDate,
    // Plafond légal ou personnalisé en centimes (null = pas de plafond configuré)
    val plafondCents: Long? = null,
    // Taux d'intérêt annuel en % (ex. 3.0 pour un Livret A à 3 %) - null si non renseigné.
    // Sert à estimer les intérêts annuels (hero Épargne) et le gain annuel par compte.
    val tauxAnnuelPct: Double? = null
)

enum class SavingsType(val displayName: String) {
    PEA              ("PEA"),
    ASSURANCE_VIE    ("Assurance vie"),
    LIVRET_A         ("Livret A"),
    LDDS             ("LDDS"),
    PEL              ("PEL"),
    PER              ("Plan Épargne Retraite"),
    ORANGE_MONEY     ("Orange Money"),
    COURTIER_EN_LIGNE("Courtier en ligne"),
    COMPTE_COURANT   ("Compte courant"),
    AUTRE            ("Autre")         // toujours en dernier - fallback de safeValueOf
}
