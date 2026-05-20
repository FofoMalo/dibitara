package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.SubCategory
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
    val currency: String,           // Ex. "EUR" — stocké en String pour la compatibilité Room
    val category: String,
    val type: String,
    val dateEpochDay: Long,         // LocalDate.toEpochDay() — évite les conversions complexes
    val note: String,
    val childId: Long? = null,      // Nullable : null si la dépense n'est pas liée à un enfant
    val isRecurring: Boolean = false,           // Ajouté en v3 : true = modèle récurrent
    val recurrenceDay: Int? = null,             // Ajouté en v3 : jour du mois (1-28, MONTHLY uniquement)
    val sourceRecurringId: Long? = null,        // Ajouté en v3 : ID du modèle qui a généré cette occurrence
    val subCategory: String? = null,            // Ajouté en v4 : sous-catégorie enum (uniquement si category == AUTRE)
    val customSubCategoryId: Long? = null,      // Ajouté en v5 : référence à custom_sub_categories
    val recurrenceFrequency: String? = null,    // Ajouté en v8 : WEEKLY | MONTHLY | YEARLY
    val firstPaymentDateEpochDay: Long? = null, // Ajouté en v8 : date première occurrence (epoch day)
    val endDateEpochDay: Long? = null           // Ajouté en v8 : date de fin (null = indéfini)
) {
    fun toDomain() = Transaction(
        id = id,
        amountCents = amountCents,
        currency = safeValueOf(currency, Currency.EUR),
        category = safeValueOf(category, Category.AUTRE),
        type = safeValueOf(type, TransactionType.EXPENSE),
        date = LocalDate.ofEpochDay(dateEpochDay),
        note = note,
        childId = childId,
        isRecurring = isRecurring,
        recurrenceDay = recurrenceDay,
        sourceRecurringId = sourceRecurringId,
        subCategory = subCategory?.let { safeValueOf(it, SubCategory.DIVERS) },
        customSubCategoryId = customSubCategoryId,
        recurrenceFrequency = recurrenceFrequency?.let { safeValueOf(it, RecurrenceFrequency.MONTHLY) },
        firstPaymentDate = firstPaymentDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
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
            childId = t.childId,
            isRecurring = t.isRecurring,
            recurrenceDay = t.recurrenceDay,
            sourceRecurringId = t.sourceRecurringId,
            subCategory = t.subCategory?.name,
            customSubCategoryId = t.customSubCategoryId,
            recurrenceFrequency = t.recurrenceFrequency?.name,
            firstPaymentDateEpochDay = t.firstPaymentDate?.toEpochDay(),
            endDateEpochDay = t.endDate?.toEpochDay()
        )
    }
}
