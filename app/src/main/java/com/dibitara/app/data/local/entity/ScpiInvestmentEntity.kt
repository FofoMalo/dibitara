package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ScpiInvestment
import java.time.LocalDate

@Entity(tableName = "scpi_investments")
data class ScpiInvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val sharesCount: Int,
    val shareValueCents: Long,
    val monthlyContributionCents: Long,
    val currency: String,
    val updatedAtEpochDay: Long
) {
    fun toDomain() = ScpiInvestment(
        id = id,
        label = label,
        sharesCount = sharesCount,
        shareValueCents = shareValueCents,
        monthlyContributionCents = monthlyContributionCents,
        currency = safeValueOf(currency, Currency.EUR),
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(s: ScpiInvestment) = ScpiInvestmentEntity(
            id = s.id,
            label = s.label,
            sharesCount = s.sharesCount,
            shareValueCents = s.shareValueCents,
            monthlyContributionCents = s.monthlyContributionCents,
            currency = s.currency.name,
            updatedAtEpochDay = s.updatedAt.toEpochDay()
        )
    }
}
