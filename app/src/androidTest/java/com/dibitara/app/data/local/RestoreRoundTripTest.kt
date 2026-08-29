package com.dibitara.app.data.local

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dibitara.app.data.export.JsonExporter
import com.dibitara.app.data.local.entity.BankAccountEntity
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import com.dibitara.app.data.local.entity.ChildEntity
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import com.dibitara.app.data.local.entity.SavingsGoalEntity
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.data.repository.RestoreRepositoryImpl
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.RestoreResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Régression : la sauvegarde JSON perdait les versements mensuels, comptes bancaires,
 * sous-catégories perso, enveloppes budgétaires et règles de catégorisation
 * (seules 11 des 18 tables étaient couvertes). Ce test fait un aller-retour
 * export → restauration et vérifie que ces collections survivent.
 */
class RestoreRoundTripTest : RoomIntegrationTestBase() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun aller_retour_conserve_les_collections_autrefois_perdues() = runBlocking<Unit> {
        // ── 1. Seed : une ligne dans chaque table "à risque" + 2 tables de base ──
        db.childDao().insert(ChildEntity.fromDomain(Child(id = 1L, name = "Nora")))
        db.transactionDao().insert(
            TransactionEntity.fromDomain(
                Transaction(
                    id = 1L, amountCents = 5000L, currency = Currency.EUR,
                    category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
                    date = LocalDate.of(2026, 8, 1)
                )
            )
        )
        db.bankAccountDao().upsert(
            BankAccountEntity.fromDomain(
                BankAccount(1L, BankProvider.BRED, "BRED", 120000L, Currency.EUR, LocalDate.of(2026, 8, 1))
            )
        )
        db.customSubCategoryDao().upsert(
            CustomSubCategoryEntity.fromDomain(CustomSubCategory(1L, "Cantine", Category.ALIMENTATION))
        )
        db.categoryEnvelopeDao().upsert(
            CategoryEnvelopeEntity.fromDomain(CategoryEnvelope(1L, Category.ALIMENTATION, 40000L, Currency.EUR))
        )
        db.categorizationRuleDao().upsert(
            CategorizationRuleEntity.fromDomain(CategorizationRule(1L, "boulangerie x", Category.ALIMENTATION))
        )
        db.monthlyVersementDao().insert(
            MonthlyVersementEntity.fromDomain(
                MonthlyVersement(1L, 1L, CompteType.EPARGNE, 2026, 8, 20000L, Currency.EUR)
            )
        )
        db.savingsGoalDao().upsert(
            SavingsGoalEntity.fromDomain(
                SavingsGoal(
                    id = 1L, name = "Voiture", targetAmountCents = 1_500_000L,
                    currentAmountCents = 420_000L, targetDate = LocalDate.of(2027, 6, 1),
                    monthlyContributionCents = 40_000L, currency = Currency.EUR,
                    colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
                )
            )
        )

        // ── 2. Export JSON (avec les mêmes valeurs que ce qu'on vient d'insérer) ──
        val exportData = ExportData(
            enfants = listOf(Child(1L, "Nora")),
            transactions = listOf(
                Transaction(
                    id = 1L, amountCents = 5000L, currency = Currency.EUR,
                    category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
                    date = LocalDate.of(2026, 8, 1)
                )
            ),
            budgets = emptyList(), epargne = emptyList(), immobilier = emptyList(),
            scpi = emptyList(), airbnb = emptyList(), vehiculeLocatif = emptyList(),
            dettes = emptyList(), actifsLibres = emptyList(), epargneSalariale = emptyList(),
            sousCategoriesPerso = listOf(CustomSubCategory(1L, "Cantine", Category.ALIMENTATION)),
            comptesBancaires = listOf(BankAccount(1L, BankProvider.BRED, "BRED", 120000L, Currency.EUR, LocalDate.of(2026, 8, 1))),
            enveloppesBudget = listOf(CategoryEnvelope(1L, Category.ALIMENTATION, 40000L, Currency.EUR)),
            reglesCategorisation = listOf(CategorizationRule(1L, "boulangerie x", Category.ALIMENTATION)),
            versementsMensuels = listOf(MonthlyVersement(1L, 1L, CompteType.EPARGNE, 2026, 8, 20000L, Currency.EUR)),
            objectifsEpargne = listOf(
                SavingsGoal(
                    id = 1L, name = "Voiture", targetAmountCents = 1_500_000L,
                    currentAmountCents = 420_000L, targetDate = LocalDate.of(2027, 6, 1),
                    monthlyContributionCents = 40_000L, currency = Currency.EUR,
                    colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
                )
            )
        )
        val fichier = File(context.cacheDir, "roundtrip-test.json").apply {
            writeText(JsonExporter.generer(exportData, "test"))
        }

        // ── 3. On simule une base repartie de zéro puis on restaure ──
        db.clearAllTables()
        val restore = RestoreRepositoryImpl(context, db)
        val result = restore.restaurer(Uri.fromFile(fichier))

        // ── 4. Vérifs ──
        assertTrue("restauration en échec : $result", result is RestoreResult.Success)
        assertEquals(1, db.monthlyVersementDao().getAll().size)
        assertEquals(1, db.bankAccountDao().getAll().first().size)
        assertEquals(1, db.categorizationRuleDao().getAll().size)
        assertEquals(1, db.customSubCategoryDao().getAll().first().size)
        assertEquals(1, db.categoryEnvelopeDao().getAll().first().size)
        assertEquals(1, db.savingsGoalDao().getAll().first().size)

        val versement = db.monthlyVersementDao().getAll().first()
        assertEquals(20000L, versement.montant_cents)
        assertEquals(2026, versement.year)
        assertEquals(8, versement.month)

        fichier.delete()
    }

    /**
     * La restauration doit VIDER `savings_goals` avant de réinsérer (via `TABLES_A_VIDER`).
     * On seede un objectif absent du fichier de sauvegarde : après restauration il ne doit
     * plus être là. Sans ce test, un oubli dans `TABLES_A_VIDER` laisserait des objectifs
     * périmés en base sans qu'aucune assertion ne le voie (le 1ᵉʳ test fait `clearAllTables()`
     * avant, ce qui masque le comportement de vidage propre à la restauration).
     */
    @Test
    fun la_restauration_vide_les_objectifs_absents_du_fichier() = runBlocking<Unit> {
        // Objectif présent en base mais PAS dans le JSON restauré
        db.savingsGoalDao().upsert(
            SavingsGoalEntity.fromDomain(
                SavingsGoal(
                    id = 99L, name = "Périmé", targetAmountCents = 100_000L,
                    currentAmountCents = 0L, targetDate = LocalDate.of(2028, 1, 1),
                    monthlyContributionCents = 0L, currency = Currency.EUR,
                    colorKey = GoalColor.OR, iconKey = GoalIcon.AUTRE
                )
            )
        )

        val exportData = ExportData(
            enfants = emptyList(), transactions = emptyList(), budgets = emptyList(),
            epargne = emptyList(), immobilier = emptyList(), scpi = emptyList(),
            airbnb = emptyList(), vehiculeLocatif = emptyList(), dettes = emptyList(),
            actifsLibres = emptyList(), epargneSalariale = emptyList(),
            sousCategoriesPerso = emptyList(), comptesBancaires = emptyList(),
            enveloppesBudget = emptyList(), reglesCategorisation = emptyList(),
            versementsMensuels = emptyList(),
            objectifsEpargne = listOf(
                SavingsGoal(
                    id = 1L, name = "Nouveau", targetAmountCents = 500_000L,
                    currentAmountCents = 0L, targetDate = LocalDate.of(2027, 1, 1),
                    monthlyContributionCents = 0L, currency = Currency.EUR,
                    colorKey = GoalColor.MAUVE, iconKey = GoalIcon.MAISON
                )
            )
        )
        val fichier = File(context.cacheDir, "roundtrip-vide-objectifs.json").apply {
            writeText(JsonExporter.generer(exportData, "test"))
        }

        val result = RestoreRepositoryImpl(context, db).restaurer(Uri.fromFile(fichier))

        assertTrue("restauration en échec : $result", result is RestoreResult.Success)
        val objectifs = db.savingsGoalDao().getAll().first()
        assertEquals(1, objectifs.size)
        assertEquals("Nouveau", objectifs.first().name)

        fichier.delete()
    }

    /**
     * Si une insertion échoue en cours de restauration (ici : contrainte UNIQUE sur
     * monthly_versements violée par deux versements identiques dans le fichier), toute
     * la transaction doit être annulée - la base d'origine reste intacte, pas à moitié
     * effacée.
     */
    @Test
    fun une_insertion_qui_echoue_annule_toute_la_restauration() = runBlocking<Unit> {
        // Base d'origine : 1 enfant + 1 versement légitime
        db.childDao().insert(ChildEntity.fromDomain(Child(id = 7L, name = "Origine")))
        db.monthlyVersementDao().insert(
            MonthlyVersementEntity.fromDomain(
                MonthlyVersement(1L, 99L, CompteType.EPARGNE, 2026, 7, 5000L, Currency.EUR)
            )
        )

        // Fichier corrompu : deux versements identiques → le 2e viole l'index UNIQUE
        val vDouble = MonthlyVersement(0L, 1L, CompteType.EPARGNE, 2026, 8, 20000L, Currency.EUR)
        val exportData = ExportData(
            enfants = listOf(Child(1L, "Nouveau")),
            transactions = emptyList(), budgets = emptyList(), epargne = emptyList(),
            immobilier = emptyList(), scpi = emptyList(), airbnb = emptyList(),
            vehiculeLocatif = emptyList(), dettes = emptyList(), actifsLibres = emptyList(),
            epargneSalariale = emptyList(), sousCategoriesPerso = emptyList(),
            comptesBancaires = emptyList(), enveloppesBudget = emptyList(),
            reglesCategorisation = emptyList(),
            versementsMensuels = listOf(vDouble, vDouble),
            objectifsEpargne = emptyList()
        )
        val fichier = File(context.cacheDir, "roundtrip-corrompu.json").apply {
            writeText(JsonExporter.generer(exportData, "test"))
        }

        val result = RestoreRepositoryImpl(context, db).restaurer(Uri.fromFile(fichier))

        assertTrue("attendu RestoreResult.Error, obtenu $result", result is RestoreResult.Error)
        // La base d'origine n'a pas été touchée
        assertEquals(1, db.childDao().getAll().first().size)
        assertEquals("Origine", db.childDao().getAll().first().first().name)
        assertEquals(1, db.monthlyVersementDao().getAll().size)
        assertEquals(99L, db.monthlyVersementDao().getAll().first().account_id)

        fichier.delete()
    }
}
