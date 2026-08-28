package com.dibitara.app.presentation.importcsv

import android.content.Context
import android.net.Uri
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import com.dibitara.app.domain.usecase.AnalyserCsvUseCase
import com.dibitara.app.domain.usecase.GetBankAccountsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.ImporterTransactionsCsvUseCase
import com.dibitara.app.domain.usecase.SuggererCategorieImportUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportCsvViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val uri: Uri = mockk(relaxed = true)

    private val ruleRepo: CategorizationRuleRepository = mockk {
        coEvery { getRuleForNote(any()) } returns null
    }
    private val importRepo: ImportRepository = mockk()
    private val prefsRepo: UserPreferencesRepository = mockk(relaxed = true)
    private val getBankAccounts: GetBankAccountsUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(emptyList())
    }
    private val getUserPreferences: GetUserPreferencesUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(UserPreferences())
    }

    private lateinit var vm: ImportCsvViewModel

    private fun fichier(contenu: String) {
        every { context.contentResolver.openInputStream(uri) } returns contenu.byteInputStream()
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        coEvery { importRepo.importerTransactions(any()) } answers { firstArg<List<Transaction>>().size }
        vm = ImportCsvViewModel(
            context = context,
            analyserCsv = AnalyserCsvUseCase(SuggererCategorieImportUseCase(ruleRepo)),
            importerCsv = ImporterTransactionsCsvUseCase(importRepo, prefsRepo),
            getBankAccounts = getBankAccounts,
            getUserPreferences = getUserPreferences,
        )
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    // ─── Fichier propre : saute le mapping, arrive à l'aperçu ────────────────

    @Test
    fun `un relevé bien formé passe directement à l'aperçu`() = runTest {
        fichier(
            """
            Date;Libellé;Montant
            01/02/2026;LECLERC;-54,20
            03/02/2026;SALAIRE;2500,00
            """.trimIndent()
        )

        vm.choisirFichier(uri)

        val etat = vm.uiState.value
        assertInstanceOf(ImportCsvUiState.Apercu::class.java, etat)
        etat as ImportCsvUiState.Apercu
        assertEquals(2, etat.transactions.size)
        assertEquals(2, etat.nouvelles)
    }

    // ─── Fichier ambigu : écran de mapping ──────────────────────────────────

    @Test
    fun `un fichier sans colonne montant reconnue demande un mapping`() = runTest {
        fichier(
            """
            Libellé;Info
            Boulangerie;RAS
            """.trimIndent()
        )

        vm.choisirFichier(uri)

        assertInstanceOf(ImportCsvUiState.MappingRequis::class.java, vm.uiState.value)
    }

    // ─── Édition de l'aperçu ────────────────────────────────────────────────

    @Test
    fun `modifierCategorie met à jour la ligne`() = runTest {
        fichier("Date;Libellé;Montant\n01/02/2026;Truc obscur;-10,00")
        vm.choisirFichier(uri)
        val tx = (vm.uiState.value as ImportCsvUiState.Apercu).transactions.single()

        vm.modifierCategorie(tx.ligneIndex, Category.LOISIRS)

        assertEquals(
            Category.LOISIRS,
            (vm.uiState.value as ImportCsvUiState.Apercu).transactions.single().category,
        )
    }

    @Test
    fun `modifierCategorie efface la sous-catégorie suggérée`() = runTest {
        coEvery { ruleRepo.getRuleForNote(any()) } returns CategorizationRule(
            noteExact = "frais bancaires",
            category = Category.AUTRE,
            customSubCategoryId = 5L,
        )
        fichier("Date;Libellé;Montant\n01/02/2026;Frais bancaires;-10,00")
        vm.choisirFichier(uri)
        val avant = (vm.uiState.value as ImportCsvUiState.Apercu).transactions.single()
        assertEquals(5L, avant.customSubCategoryId)

        vm.modifierCategorie(avant.ligneIndex, Category.LOISIRS)

        val apres = (vm.uiState.value as ImportCsvUiState.Apercu).transactions.single()
        assertEquals(Category.LOISIRS, apres.category)
        assertEquals(null, apres.customSubCategoryId)
        assertEquals(null, apres.subCategory)
    }

    @Test
    fun `basculerInclusion décoche une ligne et la retire du compteur`() = runTest {
        fichier("Date;Libellé;Montant\n01/02/2026;A;-10,00\n02/02/2026;B;-20,00")
        vm.choisirFichier(uri)
        val premier = (vm.uiState.value as ImportCsvUiState.Apercu).transactions.first()

        vm.basculerInclusion(premier.ligneIndex)

        assertEquals(1, (vm.uiState.value as ImportCsvUiState.Apercu).nouvelles)
    }

    // ─── Confirmation ───────────────────────────────────────────────────────

    @Test
    fun `confirmer insère et passe à Termine`() = runTest {
        fichier("Date;Libellé;Montant\n01/02/2026;A;-10,00\n02/02/2026;B;-20,00")
        vm.choisirFichier(uri)

        vm.confirmer()

        val etat = vm.uiState.value
        assertInstanceOf(ImportCsvUiState.Termine::class.java, etat)
        assertEquals(2, (etat as ImportCsvUiState.Termine).resultat.importees)
    }

    @Test
    fun `une erreur du repository passe à Erreur`() = runTest {
        coEvery { importRepo.importerTransactions(any()) } throws RuntimeException("db")
        fichier("Date;Libellé;Montant\n01/02/2026;A;-10,00")
        vm.choisirFichier(uri)

        vm.confirmer()

        assertInstanceOf(ImportCsvUiState.Erreur::class.java, vm.uiState.value)
    }

    // ─── Fichier vide ───────────────────────────────────────────────────────

    @Test
    fun `un fichier vide passe à Erreur`() = runTest {
        fichier("")
        vm.choisirFichier(uri)
        assertTrue(vm.uiState.value is ImportCsvUiState.Erreur)
    }
}
