package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de [SuggererCategorieImportUseCase].
 *
 * Priorité attendue : règle apprise (si elle porte une vraie catégorisation) →
 * dictionnaire [CategoriseurLibelle] → [Category.AUTRE].
 */
class SuggererCategorieImportUseCaseTest {

    private val ruleRepo: CategorizationRuleRepository = mockk()
    private val useCase = SuggererCategorieImportUseCase(ruleRepo)

    private fun aucuneRegle() = coEvery { ruleRepo.getRuleForNote(any()) } returns null
    private fun regle(rule: CategorizationRule) =
        coEvery { ruleRepo.getRuleForNote(any()) } returns rule

    @Test
    fun `note vide - AUTRE sans consulter les règles`() = runTest {
        assertEquals(Category.AUTRE, useCase("   "))
    }

    @Test
    fun `règle apprise avec une vraie catégorie - la catégorie de la règle prime`() = runTest {
        regle(CategorizationRule(noteExact = "loyer avril", category = Category.LOGEMENT))
        assertEquals(Category.LOGEMENT, useCase("LOYER AVRIL VIR-2026-04"))
    }

    @Test
    fun `règle AUTRE + sous-catégorie personnalisée - reste AUTRE, pas de retour au dictionnaire`() = runTest {
        // Le libellé matcherait "loyer" -> LOGEMENT si on retombait sur le dictionnaire.
        // La règle porte une sous-catégorie perso (customSubCategoryId) : c'est un choix
        // explicite de l'utilisateur, la catégorie principale AUTRE doit être respectée.
        regle(
            CategorizationRule(
                noteExact = "retrait loyer",
                category = Category.AUTRE,
                subCategory = null,
                customSubCategoryId = 7L,
            )
        )
        assertEquals(Category.AUTRE, useCase("RETRAIT LOYER DAB-001"))
    }

    @Test
    fun `règle AUTRE + SubCategory fixe - reste AUTRE`() = runTest {
        regle(
            CategorizationRule(
                noteExact = "frais tenue de compte",
                category = Category.AUTRE,
                subCategory = SubCategory.FRAIS_BANCAIRES,
            )
        )
        assertEquals(Category.AUTRE, useCase("FRAIS TENUE DE COMPTE"))
    }

    @Test
    fun `règle AUTRE sans aucune sous-catégorisation - ignorée, retour au dictionnaire`() = runTest {
        regle(CategorizationRule(noteExact = "leclerc courses", category = Category.AUTRE))
        assertEquals(Category.ALIMENTATION, useCase("LECLERC COURSES"))
    }

    @Test
    fun `pas de règle - le dictionnaire générique décide`() = runTest {
        aucuneRegle()
        assertEquals(Category.LOGEMENT, useCase("PRLV EDF ENERGIE"))
    }

    @Test
    fun `pas de règle et aucun mot-clé - AUTRE`() = runTest {
        aucuneRegle()
        assertEquals(Category.AUTRE, useCase("XYZ REFERENCE 12345"))
    }
}
