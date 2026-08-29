package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id                  : Long = 0,
    val provider            : String,
    val label               : String,
    val currentBalanceCents : Long,
    val currency            : String,
    val updatedAtEpochDay   : Long
) {
    fun toDomain() = BankAccount(
        id                  = id,
        provider            = safeValueOf(provider, BankProvider.AUTRE),
        label               = label,
        currentBalanceCents = currentBalanceCents,
        currency            = safeValueOf(currency, Currency.EUR),
        updatedAt           = LocalDate.ofEpochDay(updatedAtEpochDay)
    )

    companion object {
        fun fromDomain(a: BankAccount) = BankAccountEntity(
            id                  = a.id,
            provider            = a.provider.name,
            label               = a.label,
            currentBalanceCents = a.currentBalanceCents,
            currency            = a.currency.name,
            updatedAtEpochDay   = a.updatedAt.toEpochDay()
        )
    }
}
