package com.dibitara.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dibitara.app.data.export.BackupJsonVerifier
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.worker.BackupWorker
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.usecase.ExporterDonneesUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class BackupState(val folder: String? = null, val automatic: Boolean = false,
    val lastSuccess: Long = 0, val error: String? = null, val busy: Boolean = false)

/** Écrit hors du cache privé. La synchronisation distante appartient à Synology Drive. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: ExporterDonneesUseCase,
    private val database: DibitaraDatabase
) {
    private val prefs = context.getSharedPreferences("external_backups", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(lireEtat())
    val state = mutableState.asStateFlow()

    private fun lireEtat() = BackupState(prefs.getString("folder", null), prefs.getBoolean("automatic", false),
        prefs.getLong("success", 0), prefs.getString("error", null))

    fun choisirDossier(uri: Uri) {
        val id = DocumentsContract.getTreeDocumentId(uri)
        require(!id.contains("Android/data", ignoreCase = true) && !id.contains("Android/obb", ignoreCase = true)) {
            "Choisissez un dossier Documents dédié, hors du stockage privé des applications."
        }
        context.contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        check(prefs.edit().putString("folder", uri.toString()).remove("error").putLong("success", 0).commit())
        mutableState.value = lireEtat()
    }

    fun activerAutomatique(active: Boolean) {
        require(!active || state.value.folder != null) { "Choisissez d’abord le dossier de sauvegarde" }
        check(prefs.edit().putBoolean("automatic", active).commit())
        mutableState.value = lireEtat()
        val wm = WorkManager.getInstance(context)
        if (active) wm.enqueueUniquePeriodicWork(BackupWorker.NAME, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS).build())
        else wm.cancelUniqueWork(BackupWorker.NAME)
        mutableState.value = lireEtat()
    }

    suspend fun sauvegarder() = withContext(Dispatchers.IO) { mutex.withLock {
        mutableState.value = lireEtat().copy(busy = true)
        var partiel: Uri? = null
        try {
            val tree = Uri.parse(requireNotNull(state.value.folder) { "Choisissez un dossier de sauvegarde" })
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            // Snapshot cohérent : les captures concurrentes attendent la fin de la collecte Room.
            val source = database.withTransaction { exporter(ExportFormat.JSON) }
            val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: error("Export JSON illisible")
            BackupJsonVerifier.verifier(bytes.toString(Charsets.UTF_8))
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val nom = "dibitara_${date}_${UUID.randomUUID()}.json"
            partiel = DocumentsContract.createDocument(context.contentResolver, parent, "application/json", "$nom.part")
                ?: error("Impossible de créer le fichier dans le dossier choisi")
            context.contentResolver.openOutputStream(partiel, "w")?.use { it.write(bytes) }
                ?: error("Impossible d’écrire la sauvegarde")
            val relu = context.contentResolver.openInputStream(partiel)?.use { it.readBytes() }
                ?: error("Impossible de relire la sauvegarde")
            check(bytes.contentEquals(relu)) { "La vérification du fichier a échoué" }
            check(DocumentsContract.renameDocument(context.contentResolver, partiel, nom) != null) {
                "Impossible de finaliser la sauvegarde"
            }
            partiel = null
            check(prefs.edit().putLong("success", System.currentTimeMillis()).remove("error").commit())
            // Aucune rotation destructive : les anciennes sauvegardes restent disponibles.
        } catch (e: Exception) {
            partiel?.let { runCatching { DocumentsContract.deleteDocument(context.contentResolver, it) } }
            if (e is kotlinx.coroutines.CancellationException) throw e
            prefs.edit().putString("error", e.message ?: "Échec de la sauvegarde").commit()
            throw e
        } finally { mutableState.value = lireEtat() }
    } }
}
