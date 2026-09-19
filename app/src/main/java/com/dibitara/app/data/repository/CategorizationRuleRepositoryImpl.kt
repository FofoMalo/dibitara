package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.CategorizationRuleDao
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CategorizationRuleRepositoryImpl @Inject constructor(
    private val dao: CategorizationRuleDao,
    private val catalog: com.dibitara.app.domain.repository.CategoryCatalogRepository
) : CategorizationRuleRepository {

    override suspend fun upsert(rule: CategorizationRule) =
        dao.upsert(CategorizationRuleEntity.fromDomain(rule))

    override suspend fun getRuleForNote(note: String): CategorizationRule? =
        dao.getByNote(note.trim().lowercase())?.toDomain()?.let { rule ->
            val c=catalog.observe().first()
            val choice=com.dibitara.app.domain.model.CategoryChoice(rule.category,rule.subCategory,rule.customSubCategoryId)
            if(c.node(choice.key)?.let { c.selectable(it) }==true)rule.copy(category=c.category(rule.category)) else null
        }

    override suspend fun getAll(): List<CategorizationRule> =
        dao.getAll().map { it.toDomain() }
}
