package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.EmployeeSavingsType
import java.time.LocalDate

@Entity(tableName = "employee_savings")
data class EmployeeSavingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id                        : Long = 0,
    val type                      : String,
    val label                     : String,
    val currentBalanceCents       : Long,
    val employerContributionCents : Long,
    val currency                  : String,
    val updatedAtEpochDay         : Long
) {
    fun toDomain() = EmployeeSavings(
        id                        = id,
        type                      = safeValueOf(type, EmployeeSavingsType.PEE),
        label                     = label,
        currentBalanceCents       = currentBalanceCents,
        employerContributionCents = employerContributionCents,
        currency                  = safeValueOf(currency, Currency.EUR),
        updatedAt                 = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(s: EmployeeSavings) = EmployeeSavingsEntity(
            id                        = s.id,
            type                      = s.type.name,
            label                     = s.label,
            currentBalanceCents       = s.currentBalanceCents,
            employerContributionCents = s.employerContributionCents,
            currency                  = s.currency.name,
            updatedAtEpochDay         = s.updatedAt.toEpochDay()
        )
    }
}
