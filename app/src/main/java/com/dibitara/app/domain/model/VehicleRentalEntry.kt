package com.dibitara.app.domain.model

import java.time.LocalDate

// Distingue une ligne de revenu (location du véhicule) d'une ligne de charge
// (entretien, assurance, carburant...). Le montant est toujours positif ;
// le signe est porté par ce type, jamais par amountCents.
enum class VehicleEntryType(val displayName: String) {
    REVENU("Revenu"),
    CHARGE("Charge")
}

data class VehicleRentalEntry(
    val id: Long = 0,
    val label: String,
    val entryType: VehicleEntryType,
    val amountCents: Long,
    val date: LocalDate,
    val currency: Currency
)
