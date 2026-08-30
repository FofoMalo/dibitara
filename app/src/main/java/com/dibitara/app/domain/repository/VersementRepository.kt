package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.MonthlyVersement
import kotlinx.coroutines.flow.Flow

interface VersementRepository {
    suspend fun save(versement: MonthlyVersement): Result<Long>
    suspend fun existsPourMois(accountId: Long, type: CompteType, year: Int, month: Int): Boolean
    fun getForAccount(accountId: Long, type: CompteType): Flow<List<MonthlyVersement>>
    suspend fun getAllPourMois(type: CompteType, year: Int, month: Int): List<MonthlyVersement>
    /** Le versement d'un compte pour un mois donné, ou null si aucun n'existe encore. */
    suspend fun getPourMoisEtCompte(accountId: Long, type: CompteType, year: Int, month: Int): MonthlyVersement?
    suspend fun update(versement: MonthlyVersement): Result<Unit>
    /** Tous les versements enregistrés - pour l'export/sauvegarde JSON. */
    suspend fun getAll(): List<MonthlyVersement>
}
