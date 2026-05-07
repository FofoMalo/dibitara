package com.dibitara.app.domain.model

data class PatrimonyOverview(
    val liquiditesCents: Long,
    val epargneCents: Long,
    val investissementsCents: Long,
    val airbnbAnnualRevenueCents: Long,
    val dettesTotalCents: Long,
    val currency: Currency
) {
    val patrimoineBrutCents: Long
        get() = liquiditesCents + epargneCents + investissementsCents + airbnbAnnualRevenueCents

    val patrimoineNetCents: Long
        get() = patrimoineBrutCents - dettesTotalCents
}
