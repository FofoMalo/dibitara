package com.dibitara.app.data.repository

import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/** Vérifie les décisions et écritures du repository ; l’atomicité réelle reste assurée par Room. */
class CategoryCatalogRepositoryTest {
    private val db=mockk<DibitaraDatabase>(relaxed=true)
    private lateinit var repo: CategoryCatalogRepositoryImpl
    private val tx=TransactionEntity(7,1250,"EUR","ALIMENTATION","EXPENSE",20000,"Marchand",externalId="csv-id",notificationExternalId="notification-id")
    @BeforeEach fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        every { db.categoryDefinitionDao().observe() } returns flowOf(emptyList())
        every { db.customSubCategoryDao().getAll() } returns flowOf(emptyList())
        every { db.transactionDao().getAll() } returns flowOf(listOf(tx))
        every { db.transactionTrashDao().getAll() } returns flowOf(emptyList())
        every { db.categoryEnvelopeDao().getAll() } returns flowOf(emptyList())
        coEvery { db.categorizationRuleDao().getAll() } returns emptyList()
        repo=CategoryCatalogRepositoryImpl(db)
    }
    @AfterEach fun cleanup() { unmockkStatic("androidx.room.RoomDatabaseKt") }
    @Test fun `renommer ne reecrit pas les transactions`() = runTest {
        repo.save(CategoryNode("c:ALIMENTATION","Courses"))
        val transactionDao = db.transactionDao()
        coVerify(exactly=0) { transactionDao.update(any()) }
        coVerify { db.categoryDefinitionDao().save(match { it.name=="Courses" && it.key=="c:ALIMENTATION" }) }
    }
    @Test fun `classement multiple conserve identifiants et choisit sous categorie`() = runTest {
        coEvery { db.transactionDao().getById(7) } returns tx
        assertEquals(1,repo.classify(setOf(7),CategoryChoice(Category.AUTRE,SubCategory.CADEAUX)))
        coVerify { db.transactionDao().update(match { it.id==7L && it.externalId=="csv-id" && it.notificationExternalId=="notification-id" && it.subCategory=="CADEAUX" && it.categoryConfirmed }) }
    }
    @Test fun `selection invalide refuse toute ecriture`() = runTest {
        coEvery { db.transactionDao().getById(7) } returns tx
        coEvery { db.transactionDao().getById(8) } returns tx.copy(id=8,type="INCOME")
        assertTrue(runCatching { repo.classify(setOf(7,8),CategoryChoice(Category.LOGEMENT)) }.isFailure)
        val transactionDao = db.transactionDao()
        coVerify(exactly=0) { transactionDao.update(any()) }
    }
    @Test fun `fusion refuse conflit enveloppes avant toute ecriture`() = runTest {
        every { db.categoryEnvelopeDao().getAll() } returns flowOf(listOf(CategoryEnvelopeEntity(1,"ALIMENTATION",10000,"EUR"),CategoryEnvelopeEntity(2,"LOISIRS",10000,"EUR")))
        val impact=repo.impact("c:ALIMENTATION")
        assertTrue(runCatching { repo.merge("c:ALIMENTATION","c:LOISIRS",impact) }.isFailure)
        val transactionDao = db.transactionDao()
        coVerify(exactly=0) { transactionDao.update(any()) }
    }
    @Test fun `fusion suit aussi regles et corbeille`() = runTest {
        coEvery { db.categorizationRuleDao().getAll() } returns listOf(CategorizationRuleEntity(1,"marchand","ALIMENTATION"))
        every { db.transactionTrashDao().getAll() } returns flowOf(listOf(TransactionTrashEntity(8,Gson().toJson(tx.copy(id=8)),10)))
        repo.merge("c:ALIMENTATION","c:LOISIRS",repo.impact("c:ALIMENTATION"))
        coVerify { db.transactionDao().update(match { it.category=="LOISIRS" && it.externalId=="csv-id" }) }
        coVerify { db.categorizationRuleDao().upsert(match { it.category=="LOISIRS" }) }
        coVerify { db.transactionTrashDao().update(match { Gson().fromJson(it.payload,TransactionEntity::class.java).category=="LOISIRS" }) }
        coVerify { db.categoryDefinitionDao().save(match { it.key=="c:ALIMENTATION" && it.archived }) }
    }
    @Test fun `suppression refuse reference uniquement en corbeille`() = runTest {
        every { db.customSubCategoryDao().getAll() } returns flowOf(listOf(CustomSubCategoryEntity(42,"Courses","ALIMENTATION")))
        every { db.transactionTrashDao().getAll() } returns flowOf(listOf(TransactionTrashEntity(8,Gson().toJson(tx.copy(id=8,customSubCategoryId=42)),10)))
        assertTrue(runCatching { repo.deleteUnused("u:42") }.isFailure)
        val subCategoryDao = db.customSubCategoryDao()
        coVerify(exactly=0) { subCategoryDao.delete(any()) }
    }
}
