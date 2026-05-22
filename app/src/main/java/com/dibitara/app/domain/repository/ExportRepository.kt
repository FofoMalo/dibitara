package com.dibitara.app.domain.repository

import android.net.Uri
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.ExportFormat

/**
 * Contrat pour la génération et le stockage du fichier d'export.
 * La couche data écrit le fichier et retourne un Uri FileProvider
 * que la couche présentation peut passer à un Intent de partage.
 */
interface ExportRepository {
    suspend fun exporter(data: ExportData, format: ExportFormat): Uri
}
