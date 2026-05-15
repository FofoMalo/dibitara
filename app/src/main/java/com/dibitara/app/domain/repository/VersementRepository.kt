package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.MonthlyVersement
import kotlinx.coroutines.flow.Flow

interface VersementRepository {
    suspend fun save(versement: MonthlyVersement): Result<Long>
    suspend fun existsPourMois(accountId: Long, type: CompteType, year: Int, month: Int): Boolean
    fun getForAccount(accountId: Long, type: CompteType): Flow<List<MonthlyVersement>>
}
