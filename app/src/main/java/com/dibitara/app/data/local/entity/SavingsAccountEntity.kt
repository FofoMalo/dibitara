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
    val updatedAtEpochDay: Long,
    // Plafond en centimes - null si non configuré (colonne ajoutée en migration v14)
    val plafondCents: Long? = null,
    // Taux d'intérêt annuel en % - null si non renseigné (colonne ajoutée en migration v24)
    val tauxAnnuelPct: Double? = null
) {
    fun toDomain() = SavingsAccount(
        id = id,
        type = safeValueOf(type, SavingsType.LIVRET_A),
        label = label,
        currentBalanceCents = currentBalanceCents,
        monthlyContributionCents = monthlyContributionCents,
        currency = safeValueOf(currency, Currency.EUR),
        childId = childId,
        updatedAt = LocalDate.ofEpochDay(updatedAtEpochDay),
        plafondCents = plafondCents,
        tauxAnnuelPct = tauxAnnuelPct
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
            updatedAtEpochDay = s.updatedAt.toEpochDay(),
            plafondCents = s.plafondCents,
            tauxAnnuelPct = s.tauxAnnuelPct
        )
    }
}
