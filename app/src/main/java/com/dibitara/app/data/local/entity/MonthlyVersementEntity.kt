package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement

/**
 * Un seul versement par compte et par mois — contrainte UNIQUE en base.
 * Cela garantit la non-rétroactivité : chaque mois n'a qu'un enregistrement,
 * qu'on ne peut pas écraser accidentellement.
 */
@Entity(
    tableName = "monthly_versements",
    indices = [Index(value = ["account_id", "account_type", "year", "month"], unique = true)]
)
data class MonthlyVersementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val account_id: Long,
    val account_type: String,
    val year: Int,
    val month: Int,
    val montant_cents: Long,
    val currency: String
) {
    fun toDomain() = MonthlyVersement(
        id            = id,
        accountId     = account_id,
        compteType    = safeValueOf(account_type, CompteType.EPARGNE),
        year          = year,
        month         = month,
        montantCents  = montant_cents,
        currency      = safeValueOf(currency, Currency.EUR)
    )

    companion object {
        fun fromDomain(v: MonthlyVersement) = MonthlyVersementEntity(
            id           = v.id,
            account_id   = v.accountId,
            account_type = v.compteType.name,
            year         = v.year,
            month        = v.month,
            montant_cents = v.montantCents,
            currency     = v.currency.name
        )
    }
}
