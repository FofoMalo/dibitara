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
    val updatedAtEpochDay : Long
) {
    fun toDomain() = CustomAsset(
        id              = id,
        label           = label,
        totalValueCents = totalValueCents,
        currency        = safeValueOf(currency, Currency.EUR),
        updatedAt       = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(a: CustomAsset) = CustomAssetEntity(
            id              = a.id,
            label           = a.label,
            totalValueCents = a.totalValueCents,
            currency        = a.currency.name,
            updatedAtEpochDay = a.updatedAt.toEpochDay()
        )
    }
}
