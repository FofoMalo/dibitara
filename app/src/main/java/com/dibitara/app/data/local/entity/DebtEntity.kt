package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import java.time.LocalDate

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val totalCents: Long,
    val monthlyPaymentCents: Long,
    val currency: String,
    val type: String,
    val updatedAtEpochDay: Long
) {
    fun toDomain() = Debt(
        id = id,
        label = label,
        totalCents = totalCents,
        monthlyPaymentCents = monthlyPaymentCents,
        currency = Currency.valueOf(currency),
        type = DebtType.valueOf(type),
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(d: Debt) = DebtEntity(
            id = d.id,
            label = d.label,
            totalCents = d.totalCents,
            monthlyPaymentCents = d.monthlyPaymentCents,
            currency = d.currency.name,
            type = d.type.name,
            updatedAtEpochDay = d.updatedAt.toEpochDay()
        )
    }
}
