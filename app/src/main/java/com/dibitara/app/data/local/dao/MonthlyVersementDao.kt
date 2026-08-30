package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyVersementDao {

    /** Insère un versement. Lance une exception si la contrainte UNIQUE (compte, type, mois) est violée. */
    @Insert
    suspend fun insert(entity: MonthlyVersementEntity): Long

    @Query("""
        SELECT COUNT(*) FROM monthly_versements
        WHERE account_id = :accountId AND account_type = :accountType
          AND year = :year AND month = :month
    """)
    suspend fun countPourMois(accountId: Long, accountType: String, year: Int, month: Int): Int

    /** Le versement d'un compte pour un mois donné, ou null si aucun n'existe encore. */
    @Query("""
        SELECT * FROM monthly_versements
        WHERE account_id = :accountId AND account_type = :accountType
          AND year = :year AND month = :month
        LIMIT 1
    """)
    suspend fun getPourMoisEtCompte(accountId: Long, accountType: String, year: Int, month: Int): MonthlyVersementEntity?

    @Update
    suspend fun update(entity: MonthlyVersementEntity)

    @Query("""
        SELECT * FROM monthly_versements
        WHERE account_id = :accountId AND account_type = :accountType
        ORDER BY year DESC, month DESC
    """)
    fun getForAccount(accountId: Long, accountType: String): Flow<List<MonthlyVersementEntity>>

    @Query("""
        SELECT * FROM monthly_versements
        WHERE account_type = :accountType AND year = :year AND month = :month
    """)
    suspend fun getAllPourMois(accountType: String, year: Int, month: Int): List<MonthlyVersementEntity>

    /** Tous les versements enregistrés - utilisé par l'export/sauvegarde JSON. */
    @Query("SELECT * FROM monthly_versements ORDER BY year DESC, month DESC")
    suspend fun getAll(): List<MonthlyVersementEntity>
}
