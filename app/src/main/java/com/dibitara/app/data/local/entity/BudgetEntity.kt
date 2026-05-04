package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Currency

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val allocatedCents: Long,
    val spentCents: Long,
    val currency: String
) {
    fun toDomain() = Budget(
        id = id,
        month = month,
        year = year,
        allocatedCents = allocatedCents,
        spentCents = spentCents,
        currency = Currency.valueOf(currency)
    )

    companion object {
        fun fromDomain(b: Budget) = BudgetEntity(
            id = b.id,
            month = b.month,
            year = b.year,
            allocatedCents = b.allocatedCents,
            spentCents = b.spentCents,
            currency = b.currency.name
        )
    }
}
