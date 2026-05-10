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
    val updatedAt: LocalDate
)

enum class SavingsType(val displayName: String) {
    PEA              ("PEA"),
    ASSURANCE_VIE    ("Assurance vie"),
    LIVRET_A         ("Livret A"),
    PER              ("Plan Épargne Retraite"),
    ORANGE_MONEY     ("Orange Money"),
    COURTIER_EN_LIGNE("Courtier en ligne"),
    COMPTE_COURANT   ("Compte courant"),
    AUTRE            ("Autre")         // toujours en dernier — fallback de safeValueOf
}
