package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.MonthlyVersementDao
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VersementRepositoryImpl @Inject constructor(
    private val dao: MonthlyVersementDao
) : VersementRepository {

    override suspend fun save(versement: MonthlyVersement): Result<Long> =
        runCatching { dao.insert(MonthlyVersementEntity.fromDomain(versement)) }

    override suspend fun existsPourMois(
        accountId: Long, type: CompteType, year: Int, month: Int
    ): Boolean = dao.countPourMois(accountId, type.name, year, month) > 0

    override fun getForAccount(accountId: Long, type: CompteType): Flow<List<MonthlyVersement>> =
        dao.getForAccount(accountId, type.name).map { list -> list.map { it.toDomain() } }
}
