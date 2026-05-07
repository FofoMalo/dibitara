package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import java.time.LocalDate

@Entity(tableName = "real_estate_assets")
data class RealEstateAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val currentValueCents: Long,
    val currency: String,
    val updatedAtEpochDay: Long
) {
    fun toDomain() = RealEstateAsset(
        id = id,
        label = label,
        currentValueCents = currentValueCents,
        currency = safeValueOf(currency, Currency.EUR),
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(r: RealEstateAsset) = RealEstateAssetEntity(
            id = r.id,
            label = r.label,
            currentValueCents = r.currentValueCents,
            currency = r.currency.name,
            updatedAtEpochDay = r.updatedAt.toEpochDay()
        )
    }
}
