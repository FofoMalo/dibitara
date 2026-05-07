package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.LocalDate

/**
 * Représentation Room de Transaction.
 * Séparer Entity (data) et model (domain) permet de changer le schéma
 * sans impacter la logique métier — et inversement.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountCents: Long,
    val currency: String,       // Ex. "EUR" — stocké en String pour la compatibilité Room
    val category: String,
    val type: String,
    val dateEpochDay: Long,     // LocalDate.toEpochDay() — évite les conversions complexes
    val note: String,
    val childId: Long? = null   // Nullable : null si la dépense n'est pas liée à un enfant
) {
    fun toDomain() = Transaction(
        id = id,
        amountCents = amountCents,
        currency = Currency.valueOf(currency),
        category = Category.valueOf(category),
        type = TransactionType.valueOf(type),
        date = LocalDate.ofEpochDay(dateEpochDay),
        note = note,
        childId = childId
    )

    companion object {
        fun fromDomain(t: Transaction) = TransactionEntity(
            id = t.id,
            amountCents = t.amountCents,
            currency = t.currency.name,
            category = t.category.name,
            type = t.type.name,
            dateEpochDay = t.date.toEpochDay(),
            note = t.note,
            childId = t.childId
        )
    }
}
