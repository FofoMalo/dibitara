package com.dibitara.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.local.entity.TransactionEntity

/**
 * Base de données Room locale.
 *
 * RÈGLE IMPORTANTE : chaque modification du schéma (ajout de colonne, nouvelle table)
 * doit s'accompagner d'une migration Room. Ne jamais incrémenter version
 * sans écrire la migration correspondante — sinon les données utilisateur sont perdues.
 * Voir : https://developer.android.com/training/data-storage/room/migrating-db-versions
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true  // Génère un JSON de schéma — committer ce fichier pour tracer les migrations
)
abstract class DibitaraDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
