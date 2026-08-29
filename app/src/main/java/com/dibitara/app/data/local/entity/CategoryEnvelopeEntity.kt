package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Currency

/**
 * Enveloppe budgétaire par catégorie - une seule par catégorie (index unique sur [category]).
 * Le plafond est permanent : il s'applique à chaque mois, pas seulement au mois de création.
 */
@Entity(
    tableName = "category_envelopes",
    indices   = [Index(value = ["category"], unique = true)]
)
data class CategoryEnvelopeEntity(
    @PrimaryKey(autoGenerate = true)
    val id           : Long   = 0,
    val category     : String,
    val plafondCents : Long,
    val currency     : String
) {
    fun toDomain() = CategoryEnvelope(
        id           = id,
        category     = safeValueOf(category, Category.AUTRE),
        plafondCents = plafondCents,
        currency     = safeValueOf(currency, Currency.EUR)
    )

    companion object {
        fun fromDomain(e: CategoryEnvelope) = CategoryEnvelopeEntity(
            id           = e.id,
            category     = e.category.name,
            plafondCents = e.plafondCents,
            currency     = e.currency.name
        )
    }
}
