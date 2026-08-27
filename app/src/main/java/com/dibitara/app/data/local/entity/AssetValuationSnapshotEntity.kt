package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import java.time.LocalDate

@Entity(
    tableName = "asset_valuation_snapshots",
    indices = [Index(value = ["assetType", "assetId", "snapshotEpochDay"])]
)
data class AssetValuationSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetType: String,
    val assetId: Long,
    val snapshotEpochDay: Long,
    val valueCents: Long,
    val currency: String
) {
    fun toDomain() = AssetValuationSnapshot(
        id           = id,
        assetType    = safeValueOf(assetType, AssetValuationType.REAL_ESTATE),
        assetId      = assetId,
        snapshotDate = LocalDate.ofEpochDay(snapshotEpochDay),
        valueCents   = valueCents,
        currency     = safeValueOf(currency, Currency.EUR)
    )

    companion object {
        fun fromDomain(s: AssetValuationSnapshot) = AssetValuationSnapshotEntity(
            id               = s.id,
            assetType        = s.assetType.name,
            assetId          = s.assetId,
            snapshotEpochDay = s.snapshotDate.toEpochDay(),
            valueCents       = s.valueCents,
            currency         = s.currency.name
        )
    }
}
