package com.dibitara.app.domain.model

import java.time.LocalDate

data class RealEstateAsset(
    val id: Long = 0,
    val label: String,
    val currentValueCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate
)

data class ScpiInvestment(
    val id: Long = 0,
    val label: String,
    val sharesCount: Int,
    val shareValueCents: Long,
    val monthlyContributionCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate
) {
    // Valeur totale = nombre de parts × valeur unitaire
    val totalValueCents: Long get() = sharesCount.toLong() * shareValueCents
}

data class AirbnbRental(
    val id: Long = 0,
    val propertyLabel: String,
    val amountCents: Long,
    val date: LocalDate,
    val currency: Currency
)
