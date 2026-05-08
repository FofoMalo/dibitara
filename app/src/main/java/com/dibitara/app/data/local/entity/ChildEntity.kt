package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Child

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
) {
    fun toDomain() = Child(id = id, name = name)

    companion object {
        fun fromDomain(c: Child) = ChildEntity(id = c.id, name = c.name)
    }
}
