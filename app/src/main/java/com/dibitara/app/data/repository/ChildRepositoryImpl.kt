package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.ChildDao
import com.dibitara.app.data.local.entity.ChildEntity
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.repository.ChildRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChildRepositoryImpl @Inject constructor(
    private val dao: ChildDao
) : ChildRepository {

    override fun getAll(): Flow<List<Child>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(child: Child): Result<Long> = runCatching {
        dao.insert(ChildEntity.fromDomain(child))
    }

    override suspend fun delete(child: Child) {
        dao.delete(ChildEntity.fromDomain(child))
    }
}
