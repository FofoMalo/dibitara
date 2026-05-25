package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.PatrimoineSnapshot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PatrimoineSnapshotRepository {
    fun getAll(): Flow<List<PatrimoineSnapshot>>
    suspend fun existsForDay(date: LocalDate): Boolean
    suspend fun save(snapshot: PatrimoineSnapshot)
}
