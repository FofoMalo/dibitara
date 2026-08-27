package com.dibitara.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dibitara.app.BuildConfig
import com.dibitara.app.data.export.CsvExporter
import com.dibitara.app.data.export.JsonExporter
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Génère le fichier d'export dans le répertoire cache de l'app,
 * puis retourne un Uri FileProvider que l'on peut passer à un Intent de partage
 * sans exposer le chemin interne du fichier.
 */
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportRepository {

    override suspend fun exporter(data: ExportData, format: ExportFormat): Uri {
        // Dossier temporaire dédié aux exports - nettoyé par Android quand le stockage est limité
        val dossierExport = File(context.cacheDir, "exports").also { it.mkdirs() }

        val (nomFichier, contenu) = when (format) {
            ExportFormat.CSV  -> Pair(
                "dibitara_export_${System.currentTimeMillis()}.csv",
                CsvExporter.generer(data)
            )
            ExportFormat.JSON -> Pair(
                "dibitara_export_${System.currentTimeMillis()}.json",
                JsonExporter.generer(data, BuildConfig.VERSION_NAME)
            )
        }

        val fichier = File(dossierExport, nomFichier)
        fichier.writeText(contenu, Charsets.UTF_8)

        // FileProvider convertit le chemin fichier en Uri sécurisé (content://)
        // L'autorité doit correspondre à celle déclarée dans AndroidManifest.xml
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            fichier
        )
    }
}
