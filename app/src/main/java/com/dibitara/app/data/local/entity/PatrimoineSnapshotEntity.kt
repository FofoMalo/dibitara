package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.PatrimoineSnapshot
import java.time.LocalDate

@Entity(tableName = "patrimoine_snapshots")
data class PatrimoineSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snapshotEpochDay: Long,
    val patrimoineBrutCents: Long,
    val patrimoineNetCents: Long,
    val currency: String
) {
    fun toDomain() = PatrimoineSnapshot(
        id                  = id,
        snapshotDate        = LocalDate.ofEpochDay(snapshotEpochDay),
        patrimoineBrutCents = patrimoineBrutCents,
        patrimoineNetCents  = patrimoineNetCents,
        currency            = safeValueOf(currency, Currency.EUR)
    )

    companion object {
        fun fromDomain(s: PatrimoineSnapshot) = PatrimoineSnapshotEntity(
            id                  = s.id,
            snapshotEpochDay    = s.snapshotDate.toEpochDay(),
            patrimoineBrutCents = s.patrimoineBrutCents,
            patrimoineNetCents  = s.patrimoineNetCents,
            currency            = s.currency.name
        )
    }
}
