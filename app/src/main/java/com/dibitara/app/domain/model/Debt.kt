package com.dibitara.app.domain.model

import java.time.LocalDate

data class Debt(
    val id: Long = 0,
    val label: String,
    val totalCents: Long,
    val monthlyPaymentCents: Long,
    val currency: Currency,
    val type: DebtType,
    val updatedAt: LocalDate
)

enum class DebtType(val displayName: String) {
    CREDIT_IMMO("Crédit immobilier"),
    CREDIT_CONSO("Crédit consommation"),
    AUTRE("Autre dette")
}
