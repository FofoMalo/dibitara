package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.CategorizationRuleDao
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject

class CategorizationRuleRepositoryImpl @Inject constructor(
    private val dao: CategorizationRuleDao
) : CategorizationRuleRepository {

    override suspend fun upsert(rule: CategorizationRule) =
        dao.upsert(CategorizationRuleEntity.fromDomain(rule))

    override suspend fun getRuleForNote(note: String): CategorizationRule? =
        dao.getByNote(note.trim().lowercase())?.toDomain()

    override suspend fun getAll(): List<CategorizationRule> =
        dao.getAll().map { it.toDomain() }
}
