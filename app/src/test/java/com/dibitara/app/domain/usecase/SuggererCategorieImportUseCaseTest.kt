package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [SuggererCategorieImportUseCase].
 *
 * Priorité attendue (déléguée à [CascadeCategorisation]) : règle apprise (si elle
 * porte une vraie catégorisation) → dictionnaire [CategoriseurLibelle] →
 * dictionnaire de sous-catégorie d'`AUTRE` → [Category.AUTRE].
 */
class SuggererCategorieImportUseCaseTest {

    private val ruleRepo: CategorizationRuleRepository = mockk()
    private val useCase = SuggererCategorieImportUseCase(ruleRepo)

    private fun aucuneRegle() = coEvery { ruleRepo.getRuleForNote(any()) } returns null
    private fun regle(rule: CategorizationRule) =
        coEvery { ruleRepo.getRuleForNote(any()) } returns rule

    @Test
    fun `note vide - AUTRE sans consulter les règles`() = runTest {
        val s = useCase("   ")
        assertEquals(Category.AUTRE, s.category)
        assertNull(s.subCategory)
        assertNull(s.customSubCategoryId)
    }

    @Test
    fun `règle apprise avec une vraie catégorie - la catégorie de la règle prime`() = runTest {
        regle(CategorizationRule(noteExact = "loyer avril", category = Category.LOGEMENT))
        assertEquals(Category.LOGEMENT, useCase("LOYER AVRIL VIR-2026-04").category)
    }

    @Test
    fun `règle AUTRE + sous-catégorie personnalisée - catégorie ET customSubCategoryId propagés`() = runTest {
        // Le libellé matcherait "loyer" -> LOGEMENT si on retombait sur le dictionnaire.
        regle(
            CategorizationRule(
                noteExact = "retrait loyer",
                category = Category.AUTRE,
                subCategory = null,
                customSubCategoryId = 7L,
            )
        )
        val s = useCase("RETRAIT LOYER DAB-001")
        assertEquals(Category.AUTRE, s.category)
        assertNull(s.subCategory)
        assertEquals(7L, s.customSubCategoryId)
    }

    @Test
    fun `règle AUTRE + SubCategory fixe - catégorie ET subCategory propagées`() = runTest {
        regle(
            CategorizationRule(
                noteExact = "frais tenue de compte",
                category = Category.AUTRE,
                subCategory = SubCategory.FRAIS_BANCAIRES,
            )
        )
        val s = useCase("FRAIS TENUE DE COMPTE")
        assertEquals(Category.AUTRE, s.category)
        assertEquals(SubCategory.FRAIS_BANCAIRES, s.subCategory)
    }

    @Test
    fun `règle AUTRE sans aucune sous-catégorisation - ignorée, retour au dictionnaire`() = runTest {
        regle(CategorizationRule(noteExact = "leclerc courses", category = Category.AUTRE))
        assertEquals(Category.ALIMENTATION, useCase("LECLERC COURSES").category)
    }

    @Test
    fun `pas de règle - le dictionnaire de catégorie principale décide`() = runTest {
        aucuneRegle()
        assertEquals(Category.LOGEMENT, useCase("PRLV EDF ENERGIE").category)
    }

    @Test
    fun `pas de règle - le dictionnaire de sous-catégorie AUTRE s'applique`() = runTest {
        aucuneRegle()
        val s = useCase("FRAIS BANCAIRES AOUT")
        assertEquals(Category.AUTRE, s.category)
        assertEquals(SubCategory.FRAIS_BANCAIRES, s.subCategory)
    }

    @Test
    fun `pas de règle et aucun mot-clé - AUTRE nu`() = runTest {
        aucuneRegle()
        val s = useCase("XYZ OPERATION 12345")
        assertEquals(Category.AUTRE, s.category)
        assertNull(s.subCategory)
        assertNull(s.customSubCategoryId)
    }
}
