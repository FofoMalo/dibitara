package com.dibitara.app.domain.model

data class PatrimonyOverview(
    val liquiditesCents: Long,
    val epargneCents: Long,
    val investissementsCents: Long,
    val airbnbAnnualRevenueCents: Long,
    val dettesTotalCents: Long,
    val currency: Currency
) {
    // Airbnb est un revenu (flux), pas un actif (stock) — exclu du patrimoine brut
    val patrimoineBrutCents: Long
        get() = liquiditesCents + epargneCents + investissementsCents

    val patrimoineNetCents: Long
        get() = patrimoineBrutCents - dettesTotalCents
}
