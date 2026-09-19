package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.CategoryNode

/** Métadonnées séparées : renommer/archiver ne modifie aucune transaction. */
@Entity(tableName = "category_definitions")
data class CategoryDefinitionEntity(
    @PrimaryKey val key: String,
    val name: String,
    val parentKey: String?,
    val archived: Boolean,
    val icon: String,
    val color: String
) {
    fun toDomain() = CategoryNode(key, name, parentKey, archived, icon, color)
    companion object { fun fromDomain(n: CategoryNode) = CategoryDefinitionEntity(n.key,n.name,n.parentKey,n.archived,n.icon,n.color) }
}
