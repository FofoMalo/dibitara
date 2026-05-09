package com.dibitara.app.domain.model

/** Résumé des dépenses (type EXPENSE) pour un mois donné. */
data class MonthlyExpense(
    val month: Int,
    val year: Int,
    val totalCents: Long
)
