package com.dibitara.app.domain.repository

import android.net.Uri

/** Résultat d'une tentative de restauration. */
sealed class RestoreResult {
    /** Restauration réussie — [nbElements] = nombre total d'entités réinsérées. */
    data class Success(val nbElements: Int) : RestoreResult()
    /** Échec — [message] décrit la cause (JSON invalide, données corrompues…). */
    data class Error(val message: String)   : RestoreResult()
}

/**
 * Contrat pour la restauration des données depuis un fichier JSON de sauvegarde.
 * L'implémentation lit le fichier via [uri], vide la base, puis réinsère toutes les entités.
 */
interface RestoreRepository {
    suspend fun restaurer(uri: Uri): RestoreResult
}
