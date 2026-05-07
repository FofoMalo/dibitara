package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import java.time.LocalDate

@Entity(tableName = "savings_accounts")
data class SavingsAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val label: String,
    val currentBalanceCents: Long,
    val monthlyContributionCents: Long,
    val currency: String,
    val childId: Long?,
    val updatedAtEpochDay: Long
) {
    fun toDomain() = SavingsAccount(
        id = id,
        type = SavingsType.valueOf(type),
        label = label,
        currentBalanceCents = currentBalanceCents,
        monthlyContributionCents = monthlyContributionCents,
        currency = Currency.valueOf(currency),
        childId = childId,
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(s: SavingsAccount) = SavingsAccountEntity(
            id = s.id,
            type = s.type.name,
            label = s.label,
            currentBalanceCents = s.currentBalanceCents,
            monthlyContributionCents = s.monthlyContributionCents,
            currency = s.currency.name,
            childId = s.childId,
            updatedAtEpochDay = s.updatedAt.toEpochDay()
        )
    }
}
