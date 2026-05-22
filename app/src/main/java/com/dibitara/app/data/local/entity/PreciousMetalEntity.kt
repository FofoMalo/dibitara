package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MetalType
import com.dibitara.app.domain.model.PreciousMetalAsset
import java.time.LocalDate

@Entity(tableName = "precious_metals")
data class PreciousMetalEntity(
    @PrimaryKey(autoGenerate = true)
    val id                : Long = 0,
    val metalType         : String,
    val label             : String,
    val quantityGrams     : Double,
    val pricePerGramCents : Long,
    val currency          : String,
    val updatedAtEpochDay : Long
) {
    fun toDomain() = PreciousMetalAsset(
        id                = id,
        metalType         = safeValueOf(metalType, MetalType.OR),
        label             = label,
        quantityGrams     = quantityGrams,
        pricePerGramCents = pricePerGramCents,
        currency          = safeValueOf(currency, Currency.EUR),
        updatedAt         = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(a: PreciousMetalAsset) = PreciousMetalEntity(
            id                = a.id,
            metalType         = a.metalType.name,
            label             = a.label,
            quantityGrams     = a.quantityGrams,
            pricePerGramCents = a.pricePerGramCents,
            currency          = a.currency.name,
            updatedAtEpochDay = a.updatedAt.toEpochDay()
        )
    }
}
