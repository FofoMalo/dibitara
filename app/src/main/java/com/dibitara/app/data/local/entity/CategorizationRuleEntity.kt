package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.SubCategory

/**
 * Ligne Room pour une règle de catégorisation apprise.
 *
 * [noteExact] est indexé en UNIQUE pour garantir qu'un libellé n'a qu'une seule règle.
 * L'upsert (OnConflictStrategy.REPLACE) écrase la règle existante si l'utilisateur
 * change d'avis sur la catégorie d'un libellé donné.
 */
@Entity(
    tableName = "categorization_rules",
    indices = [Index(value = ["noteExact"], unique = true)]
)
data class CategorizationRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteExact: String,
    val category: String,
    val subCategory: String? = null,
    val customSubCategoryId: Long? = null
) {
    fun toDomain() = CategorizationRule(
        id                   = id,
        noteExact            = noteExact,
        category             = safeValueOf(category, Category.AUTRE),
        subCategory          = subCategory?.let { safeValueOf(it, SubCategory.DIVERS) },
        customSubCategoryId  = customSubCategoryId
    )

    companion object {
        fun fromDomain(rule: CategorizationRule) = CategorizationRuleEntity(
            id                   = rule.id,
            noteExact            = rule.noteExact,
            category             = rule.category.name,
            subCategory          = rule.subCategory?.name,
            customSubCategoryId  = rule.customSubCategoryId
        )
    }
}
