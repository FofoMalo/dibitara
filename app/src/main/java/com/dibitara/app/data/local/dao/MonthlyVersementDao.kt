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

    @Query("""
        SELECT * FROM monthly_versements
        WHERE account_id = :accountId AND account_type = :accountType
        ORDER BY year DESC, month DESC
    """)
    fun getForAccount(accountId: Long, accountType: String): Flow<List<MonthlyVersementEntity>>
}
