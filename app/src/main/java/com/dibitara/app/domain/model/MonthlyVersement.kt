package com.dibitara.app.domain.model

/**
 * Enregistrement d'un versement mensuel effectué sur un compte épargne ou une SCPI.
 * Un seul versement est autorisé par compte et par mois (contrainte unique en base).
 * La non-rétroactivité est assurée par le fait qu'on enregistre [montantCents] au
 * moment du versement - modifier le montant prévu sur le compte ne change pas les
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

/**
 * Distingue les versements épargne (SavingsAccount), SCPI (ScpiInvestment), épargne
 * salariale (EmployeeSavings) et objectif d'épargne (SavingsGoal).
 *
 * Stockée en base comme `String` dans `monthly_versements.account_type` (lue en
 * `safeValueOf`) : ajouter une valeur ne nécessite **aucune migration Room**.
 * Pour un versement `OBJECTIF`, [MonthlyVersement.accountId] porte l'id du
 * `SavingsGoal` (comme pour SCPI/EMPLOYEE_SAVINGS qui pointent leurs propres tables).
 */
enum class CompteType { EPARGNE, SCPI, EMPLOYEE_SAVINGS, OBJECTIF }
