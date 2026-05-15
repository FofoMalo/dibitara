package com.dibitara.app.domain.model

/**
 * Enregistrement d'un versement mensuel effectué sur un compte épargne ou une SCPI.
 * Un seul versement est autorisé par compte et par mois (contrainte unique en base).
 * La non-rétroactivité est assurée par le fait qu'on enregistre [montantCents] au
 * moment du versement — modifier le montant prévu sur le compte ne change pas les
 * enregistrements passés.
 */
data class MonthlyVersement(
    val id: Long = 0,
    val accountId: Long,
    val compteType: CompteType,
    val year: Int,
    val month: Int,
    val montantCents: Long,
    val currency: Currency
)

/** Distingue les versements épargne (SavingsAccount) des versements SCPI (ScpiInvestment). */
enum class CompteType { EPARGNE, SCPI }
