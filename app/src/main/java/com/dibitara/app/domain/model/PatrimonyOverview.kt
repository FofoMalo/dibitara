package com.dibitara.app.domain.model

data class PatrimonyOverview(
    val liquiditesCents: Long,
    val epargneCents: Long,
    val investissementsCents: Long,
    val airbnbAnnualRevenueCents: Long,
    val vehicleRentalNetRevenueCents: Long,
    val dettesTotalCents: Long,
    val currency: Currency,
    /** True si au moins un montant agrégé ci-dessus a réellement été converti depuis une autre devise. */
    val hasConvertedValues: Boolean = false
) {
    // Airbnb et le véhicule locatif sont des revenus (flux), pas des actifs (stock) - exclus du patrimoine brut
    val patrimoineBrutCents: Long
        get() = liquiditesCents + epargneCents + investissementsCents

    val patrimoineNetCents: Long
        get() = patrimoineBrutCents - dettesTotalCents
}
