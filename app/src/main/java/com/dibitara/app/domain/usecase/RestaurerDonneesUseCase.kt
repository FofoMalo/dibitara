package com.dibitara.app.domain.usecase

import android.net.Uri
import com.dibitara.app.domain.repository.RestoreRepository
import com.dibitara.app.domain.repository.RestoreResult
import javax.inject.Inject

/**
 * Restaure toutes les données de l'application depuis un fichier JSON de sauvegarde.
 *
 * Ce UseCase est l'inverse de [ExporterDonneesUseCase] :
 * l'utilisateur choisit un fichier produit par l'export JSON,
 * et les données sont réinsérées dans Room après une remise à zéro de la base.
 *
 * @param uri URI SAF du fichier JSON sélectionné par l'utilisateur via le file picker Android.
 * @return [RestoreResult.Success] avec le nombre d'entités restaurées,
 *         ou [RestoreResult.Error] si le fichier est invalide ou la lecture échoue.
 */
class RestaurerDonneesUseCase @Inject constructor(
    private val repository: RestoreRepository
) {
    suspend operator fun invoke(uri: Uri): RestoreResult = repository.restaurer(uri)
}
