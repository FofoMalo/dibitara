package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "airbnb_rentals")
data class AirbnbRentalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val propertyLabel: String,
    val amountCents: Long,
    val dateEpochDay: Long,
    val currency: String
) {
    fun toDomain() = AirbnbRental(
        id = id,
        propertyLabel = propertyLabel,
        amountCents = amountCents,
        date = LocalDate.ofEpochDay(dateEpochDay),
        currency = Currency.valueOf(currency)
    )

    companion object {
        fun fromDomain(r: AirbnbRental) = AirbnbRentalEntity(
            id = r.id,
            propertyLabel = r.propertyLabel,
            amountCents = r.amountCents,
            dateEpochDay = r.date.toEpochDay(),
            currency = r.currency.name
        )
    }
}
