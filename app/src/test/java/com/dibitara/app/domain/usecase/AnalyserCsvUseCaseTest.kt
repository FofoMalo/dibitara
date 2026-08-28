package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ModeMontant
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalyserCsvUseCaseTest {

    // Aucune règle apprise : on teste la suggestion via le dictionnaire générique seul.
    private val ruleRepo: CategorizationRuleRepository = mockk<CategorizationRuleRepository>().also {
        coEvery { it.getRuleForNote(any()) } returns null
    }
    private val useCase = AnalyserCsvUseCase(SuggererCategorieImportUseCase(ruleRepo))

    private fun lignes(vararg l: String) = l.toList()

    // ─── CSV propre : mapping deviné, transactions parsées ────────────────────

    @Test
    fun `relevé FR bien formé - aperçu complet`() = runTest {
        val preview = useCase(
            lignes(
                "Date;Libellé;Montant",
                "01/02/2026;LECLERC COURSES;-54,20",
                "03/02/2026;VIREMENT SALAIRE;2500,00",
                "05/02/2026;PHARMACIE DU CENTRE;-18,90",
            ),
            deviseParDefaut = Currency.EUR,
        )

        assertTrue(preview.mapping.estComplet)
        assertEquals(3, preview.transactions.size)
        assertEquals(0, preview.lignesIgnorees)

        // Catégorie suggérée par le dictionnaire générique
        val courses = preview.transactions.first { it.note.contains("LECLERC") }
        assertEquals(Category.ALIMENTATION, courses.category)
        assertEquals(TransactionType.EXPENSE, courses.type)
    }

    // ─── CSV incomplet : pas de transactions, mapping renvoyé pour l'écran ────

    @Test
    fun `colonne montant manquante - aperçu sans transactions`() = runTest {
        val preview = useCase(
            lignes(
                "Date;Libellé",
                "01/02/2026;Boulangerie",
            ),
            deviseParDefaut = Currency.EUR,
        )

        assertFalse(preview.mapping.estComplet)
        assertTrue(preview.transactions.isEmpty())
        assertEquals(listOf("Date", "Libellé"), preview.enTetes)
    }

    // ─── Lignes ignorées comptées ────────────────────────────────────────────

    @Test
    fun `les lignes non parseables sont comptées séparément`() = runTest {
        val preview = useCase(
            lignes(
                "Date;Libellé;Montant",
                "01/02/2026;OK;-10,00",
                "date bidon;KO;-10,00",
                "02/02/2026;Montant nul;0,00",
            ),
            deviseParDefaut = Currency.EUR,
        )

        assertEquals(1, preview.transactions.size)
        assertEquals(2, preview.lignesIgnorees)
    }

    // ─── Transactions jumelles : deux lignes UI, un seul id de dédup ─────────

    @Test
    fun `deux lignes identiques le même jour restent distinctes dans l'aperçu`() = runTest {
        val preview = useCase(
            lignes(
                "Date;Libellé;Montant",
                "01/02/2026;CAFE;-2,00",
                "01/02/2026;CAFE;-2,00",
            ),
            deviseParDefaut = Currency.EUR,
        )

        assertEquals(2, preview.transactions.size)
        // clé d'affichage unique par ligne (sinon LazyColumn plante sur clé dupliquée)
        assertEquals(2, preview.transactions.map { it.ligneIndex }.toSet().size)
        // mais même externalId : la dédup à l'insertion n'en gardera qu'une
        assertEquals(1, preview.transactions.map { it.externalId }.toSet().size)
    }

    // ─── Sous-catégorie apprise propagée aux transactions importées ──────────

    @Test
    fun `une règle apprise « AUTRE + sous-catégorie perso » est portée par la transaction importée`() = runTest {
        val repoAvecRegle = mockk<CategorizationRuleRepository>().also {
            coEvery { it.getRuleForNote(any()) } returns com.dibitara.app.domain.model.CategorizationRule(
                noteExact = "retrait dab",
                category = Category.AUTRE,
                customSubCategoryId = 3L,
            )
        }
        val useCaseAvecRegle = AnalyserCsvUseCase(SuggererCategorieImportUseCase(repoAvecRegle))

        val preview = useCaseAvecRegle(
            lignes(
                "Date;Libellé;Montant",
                "01/02/2026;RETRAIT DAB;-50,00",
            ),
            deviseParDefaut = Currency.EUR,
        )

        val tx = preview.transactions.single()
        assertEquals(Category.AUTRE, tx.category)
        assertEquals(3L, tx.customSubCategoryId)
        assertEquals(3L, tx.toTransaction().customSubCategoryId)
    }

    // ─── mappingImpose court-circuite l'auto-détection ───────────────────────

    @Test
    fun `un mapping imposé est utilisé tel quel`() = runTest {
        // Fichier sans en-tête, colonnes dans un ordre inhabituel : Montant ; Date ; Libellé
        val mapping = com.dibitara.app.domain.model.CsvColumnMapping(
            delimiteur = ',',
            aEnTete = false,
            colonneDate = 1,
            formatDate = "yyyy-MM-dd",
            modeMontant = ModeMontant.COLONNE_SIGNEE,
            colonneMontant = 0,
            separateurDecimal = '.',
            colonnesLibelle = listOf(2),
        )

        val preview = useCase(
            lignes("-30.00,2026-02-01,Cafe"),
            deviseParDefaut = Currency.EUR,
            mappingImpose = mapping,
        )

        assertEquals(1, preview.transactions.size)
        val t = preview.transactions.single()
        assertEquals(3000L, t.amountCents)
        assertEquals("Cafe", t.note)
        assertEquals(TransactionType.EXPENSE, t.type)
    }
}
