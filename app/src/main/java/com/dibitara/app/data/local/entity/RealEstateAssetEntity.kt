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
    val updatedAtEpochDay: Long,
    val debtId: Long? = null,
    val acquisitionValueCents: Long? = null,
    val acquisitionDateEpochDay: Long? = null
) {
    fun toDomain() = RealEstateAsset(
        id = id,
        label = label,
        currentValueCents = currentValueCents,
        currency = safeValueOf(currency, Currency.EUR),
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay),
        debtId = debtId,
        acquisitionValueCents = acquisitionValueCents,
        acquisitionDate = acquisitionDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    )

    companion object {
        fun fromDomain(r: RealEstateAsset) = RealEstateAssetEntity(
            id = r.id,
            label = r.label,
            currentValueCents = r.currentValueCents,
            currency = r.currency.name,
            updatedAtEpochDay = r.updatedAt.toEpochDay(),
            debtId = r.debtId,
            acquisitionValueCents = r.acquisitionValueCents,
            acquisitionDateEpochDay = r.acquisitionDate?.toEpochDay()
        )
    }
}
