package com.dibitara.app.data.export

import android.content.Context
import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.dibitara.app.data.backup.BackupManager
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.usecase.ExporterDonneesUseCase
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream

class BackupManagerTest {
    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>()
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val database = mockk<DibitaraDatabase>()
    private val exporter = mockk<ExporterDonneesUseCase>()
    private val tree = mockk<Uri>(); private val parent = mockk<Uri>()
    private val source = mockk<Uri>(); private val part = mockk<Uri>(); private val final = mockk<Uri>()
    private lateinit var manager: BackupManager
    private val output = ByteArrayOutputStream()
    private val json = """{"version":"4.10.0","exportDate":"2026-09-10","enfants":[],"transactions":[],"budgets":[],"epargne":[],"immobilier":[],"scpi":[],"airbnb":[],"vehicule_locatif":[],"dettes":[],"actifs_libres":[],"epargne_salariale":[],"sous_categories_perso":[],"comptes_bancaires":[],"enveloppes_budget":[],"regles_categorisation":[],"versements_mensuels":[],"objectifs_epargne":[]}"""

    @BeforeEach fun prepare() {
        mockkStatic(Uri::class); mockkStatic(DocumentsContract::class); mockkStatic("androidx.room.RoomDatabaseKt")
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.contentResolver } returns resolver
        every { prefs.getString("folder", null) } returns "folder"
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true
        every { Uri.parse("folder") } returns tree
        every { DocumentsContract.getTreeDocumentId(tree) } returns "primary:Documents/Backups"
        every { DocumentsContract.buildDocumentUriUsingTree(tree, any()) } returns parent
        every { DocumentsContract.createDocument(resolver, parent, any(), any()) } returns part
        every { DocumentsContract.renameDocument(resolver, part, any()) } returns final
        every { DocumentsContract.deleteDocument(resolver, part) } returns true
        coEvery { database.withTransaction<Uri>(any()) } coAnswers { secondArg<suspend () -> Uri>().invoke() }
        coEvery { exporter(ExportFormat.JSON) } returns source
        every { resolver.openInputStream(source) } answers { json.byteInputStream() }
        every { resolver.openOutputStream(part, "w") } returns output
        every { resolver.openInputStream(part) } answers { output.toByteArray().inputStream() }
        manager = BackupManager(context, exporter, database)
    }
    @AfterEach fun cleanup() { unmockkAll() }

    @Test fun `succès seulement après relecture et finalisation`() = runTest {
        manager.sauvegarder()
        assertEquals(json, output.toString(Charsets.UTF_8.name()))
        verifyOrder {
            resolver.openInputStream(part)
            DocumentsContract.renameDocument(resolver, part, match { it.endsWith(".json") })
            editor.putLong("success", any())
        }
        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test fun `relecture différente refuse le succès et ne nettoie que sa copie partielle`() = runTest {
        every { resolver.openInputStream(part) } answers { "tronqué".byteInputStream() }
        assertTrue(runCatching { manager.sauvegarder() }.isFailure)
        verify(exactly = 0) { editor.putLong("success", any()) }
        verify(exactly = 0) { DocumentsContract.renameDocument(any(), any(), any()) }
        verify(exactly = 1) { DocumentsContract.deleteDocument(resolver, part) }
    }

    @Test fun `écriture refusée conserve la dernière date de succès`() = runTest {
        every { resolver.openOutputStream(part, "w") } throws SecurityException("Accès retiré")
        assertTrue(runCatching { manager.sauvegarder() }.isFailure)
        verify(exactly = 0) { editor.putLong("success", any()) }
        verify { editor.putString("error", "Accès retiré") }
    }
}
