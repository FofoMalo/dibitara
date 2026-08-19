package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomAsset
import java.time.LocalDate

@Entity(tableName = "custom_assets")
data class CustomAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id              : Long = 0,
    val label           : String,
    val totalValueCents : Long,
    val currency        : String,
    val updatedAtEpochDay : Long,
    val acquisitionValueCents : Long? = null,
    val acquisitionDateEpochDay : Long? = null
) {
    fun toDomain() = CustomAsset(
        id              = id,
        label           = label,
        totalValueCents = totalValueCents,
        currency        = safeValueOf(currency, Currency.EUR),
        updatedAt       = LocalDate.ofEpochDay(updatedAtEpochDay),
        acquisitionValueCents = acquisitionValueCents,
        acquisitionDate       = acquisitionDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    )

    companion object {
        fun fromDomain(a: CustomAsset) = CustomAssetEntity(
            id              = a.id,
            label           = a.label,
            totalValueCents = a.totalValueCents,
            currency        = a.currency.name,
            updatedAtEpochDay = a.updatedAt.toEpochDay(),
            acquisitionValueCents = a.acquisitionValueCents,
            acquisitionDateEpochDay = a.acquisitionDate?.toEpochDay()
        )
    }
}
