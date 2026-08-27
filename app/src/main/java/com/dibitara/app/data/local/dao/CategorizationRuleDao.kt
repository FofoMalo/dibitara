package com.dibitara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dibitara.app.data.local.entity.CategorizationRuleEntity

@Dao
interface CategorizationRuleDao {

    /**
     * Insère ou remplace la règle pour ce libellé.
     * Le UNIQUE index sur noteExact garantit un seul enregistrement par libellé.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategorizationRuleEntity)

    /** Cherche une règle par libellé exact (stocké en minuscules). */
    @Query("SELECT * FROM categorization_rules WHERE noteExact = :noteExact LIMIT 1")
    suspend fun getByNote(noteExact: String): CategorizationRuleEntity?
}
