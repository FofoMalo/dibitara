package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CustomSubCategory

/**
 * Ligne Room pour une sous-catégorie personnalisée.
 * [parentCategory] est stocké en String (nom de l'enum [Category])
 * — même convention que TransactionEntity.category.
 */
@Entity(tableName = "custom_sub_categories")
data class CustomSubCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentCategory: String   // ex. "ALIMENTATION", "LOISIRS"…
) {
    fun toDomain() = CustomSubCategory(
        id             = id,
        name           = name,
        parentCategory = safeValueOf(parentCategory, Category.AUTRE)
    )

    companion object {
        fun fromDomain(c: CustomSubCategory) = CustomSubCategoryEntity(
            id             = c.id,
            name           = c.name,
            parentCategory = c.parentCategory.name
        )
    }
}
