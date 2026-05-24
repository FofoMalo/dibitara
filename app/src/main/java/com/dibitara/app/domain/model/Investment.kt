package com.dibitara.app.domain.model

import java.time.LocalDate

data class RealEstateAsset(
    val id: Long = 0,
    val label: String,
    val currentValueCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate,
    // Référence optionnelle vers la dette (crédit) qui finance ce bien
    val debtId: Long? = null
)

data class ScpiInvestment(
    val id: Long = 0,
    val label: String,
    val sharesCount: Double,
    val shareValueCents: Long,
    val monthlyContributionCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate
) {
    // Valeur totale = nombre de parts (peut être fractionnaire, ex : 2,2) × valeur unitaire
    val totalValueCents: Long get() = (sharesCount * shareValueCents).toLong()
}

data class AirbnbRental(
    val id: Long = 0,
    val propertyLabel: String,
    val amountCents: Long,
    val date: LocalDate,
    val currency: Currency
)
