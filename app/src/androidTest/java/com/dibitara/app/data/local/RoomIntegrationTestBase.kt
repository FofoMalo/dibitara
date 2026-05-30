package com.dibitara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dibitara.app.data.local.database.DibitaraDatabase
import org.junit.After
import org.junit.Before

/**
 * Classe de base pour les tests d'intégration Room.
 * Crée une base de données en mémoire avant chaque test et la ferme après.
 * [allowMainThreadQueries] est autorisé uniquement en test — jamais en production.
 */
abstract class RoomIntegrationTestBase {

    protected lateinit var db: DibitaraDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DibitaraDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }
}
