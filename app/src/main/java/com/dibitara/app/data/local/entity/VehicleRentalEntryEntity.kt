package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import java.time.LocalDate

@Entity(tableName = "vehicle_rental_entries")
data class VehicleRentalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val entryType: String,
    val amountCents: Long,
    val dateEpochDay: Long,
    val currency: String
) {
    fun toDomain() = VehicleRentalEntry(
        id = id,
        label = label,
        entryType = safeValueOf(entryType, VehicleEntryType.REVENU),
        amountCents = amountCents,
        date = LocalDate.ofEpochDay(dateEpochDay),
        currency = safeValueOf(currency, Currency.EUR)
    )

    companion object {
        fun fromDomain(e: VehicleRentalEntry) = VehicleRentalEntryEntity(
            id = e.id,
            label = e.label,
            entryType = e.entryType.name,
            amountCents = e.amountCents,
            dateEpochDay = e.date.toEpochDay(),
            currency = e.currency.name
        )
    }
}
