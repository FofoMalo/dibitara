package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
)
