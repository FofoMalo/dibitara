package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.data.export.CategoryJsonAdapter
import com.dibitara.app.data.local.entity.TransactionEntity
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CategoryCatalogTest {
    private val custom = "USER_12345678-1234-1234-1234-123456789abc"
    @Test fun `renommer conserve egalite et regroupement`() {
        val before=Category(custom,"Voyages")
        val after=Category(custom,"Vacances")
        assertEquals(before,after)
        assertEquals(before.hashCode(),after.hashCode())
        assertEquals(1,listOf(before,after).groupBy { it }.size)
        assertNotEquals(Category.AUTRE,after)
    }
    @Test fun `identifiants historiques json et room restent compatibles`() {
        val gson=GsonBuilder().registerTypeAdapter(Category::class.java,CategoryJsonAdapter()).create()
        assertEquals("\"ALIMENTATION\"",gson.toJson(Category.ALIMENTATION))
        assertEquals(Category.ALIMENTATION,gson.fromJson("\"ALIMENTATION\"",Category::class.java))
        val tx=Transaction(7,1250,Currency.EUR,Category.valueOf(custom),TransactionType.EXPENSE,LocalDate.of(2026,9,14),categoryConfirmed=true)
        assertEquals(tx,TransactionEntity.fromDomain(tx).toDomain())
        assertEquals(tx.category,gson.fromJson(gson.toJson(tx.category),Category::class.java))
    }
    @Test fun `sous categorie fixe peut changer de parent sans changer identite`() {
        val c=CategoryCatalog(CategoryCatalog.defaults().map { if(it.key=="s:BAR_ET_RESTAURANT")it.copy(parentKey="c:ALIMENTATION",name="Restaurants")else it })
        val choice=c.choice("s:BAR_ET_RESTAURANT")
        assertEquals(Category.ALIMENTATION,choice.category)
        assertEquals(SubCategory.BAR_ET_RESTAURANT,choice.subCategory)
        assertEquals("Alimentation › Restaurants",c.label(choice))
    }
    @Test fun `archive du parent masque ses sous categories sans perdre leur libelle`() {
        val c=CategoryCatalog(CategoryCatalog.defaults().map { if(it.key=="c:AUTRE")it.copy(archived=true)else it })
        assertFalse(c.selectable(c.node("s:CADEAUX")!!))
        assertEquals("Autre › Cadeaux",c.label(c.choice("s:CADEAUX")))
    }
    @Test fun `catalogue personnel resout nom couleur et icone`() {
        val c=CategoryCatalog(CategoryCatalog.defaults()+CategoryNode("c:$custom","Animaux",icon="HEART",color="TEAL"))
        val cat=c.category(Category.valueOf(custom))
        assertEquals("Animaux",cat.displayName)
        assertEquals("TEAL",cat.color)
        assertEquals("HEART",cat.icon)
    }
    @Test fun `autre volontaire ne figure pas dans a categoriser`() {
        val tx=Transaction(7,1250,Currency.EUR,Category.AUTRE,TransactionType.EXPENSE,LocalDate.of(2026,9,14))
        val filter=com.dibitara.app.presentation.expenses.ExpensesFilter(uncategorizedOnly=true)
        assertEquals(listOf(tx),filter.apply(listOf(tx,tx.copy(id=8,categoryConfirmed=true))))
    }
    @Test fun `filtre sous categorie ne melange pas les autres sous categories`() {
        val tx=Transaction(7,1250,Currency.EUR,Category.ALIMENTATION,TransactionType.EXPENSE,LocalDate.of(2026,9,14),customSubCategoryId=42)
        val filter=com.dibitara.app.presentation.expenses.ExpensesFilter(category=Category.ALIMENTATION,customSubCategoryId=42)
        assertEquals(listOf(tx),filter.apply(listOf(tx,tx.copy(id=8,customSubCategoryId=43))))
    }
    @Test fun `impact interdit suppression quand corbeille seule utilise la categorie`() {
        assertFalse(CategoryImpact(0,0,0,1,0).isUnused)
        assertFalse(CategoryImpact(0,0,0,0,1).isUnused)
        assertTrue(CategoryImpact(0,0,0,0,0).isUnused)
    }
    @Test fun `recherche sans accents et mots dans ordre libre`() {
        assertTrue("Épargne › Études".matchesCategorySearch("etudes epargne"))
        assertFalse("Transport › Carburant".matchesCategorySearch("restaurant"))
    }
    @Test fun `catalogue refuse parent absent avant restauration`() {
        assertThrows(IllegalArgumentException::class.java) { CategoryCatalog(CategoryCatalog.defaults()+CategoryNode("u:42","Courses","c:ABSENT")).validate() }
    }
    @Test fun `catalogue refuse un troisieme niveau`() {
        assertThrows(IllegalArgumentException::class.java) { CategoryCatalog(CategoryCatalog.defaults()+CategoryNode("u:42","Courses","s:CADEAUX")).validate() }
    }
}
