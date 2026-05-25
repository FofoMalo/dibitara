package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.PatrimoineSnapshotDao
import com.dibitara.app.data.local.entity.PatrimoineSnapshotEntity
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class PatrimoineSnapshotRepositoryImpl @Inject constructor(
    private val dao: PatrimoineSnapshotDao
) : PatrimoineSnapshotRepository {

    override fun getAll(): Flow<List<PatrimoineSnapshot>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun existsForDay(date: LocalDate): Boolean =
        dao.countForDay(date.toEpochDay()) > 0

    override suspend fun save(snapshot: PatrimoineSnapshot) {
        dao.insert(PatrimoineSnapshotEntity.fromDomain(snapshot))
    }
}
