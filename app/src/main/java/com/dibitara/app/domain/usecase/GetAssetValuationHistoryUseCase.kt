package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retourne l'historique de valorisation d'un actif : un point par mois (le
 * dernier snapshot du mois), trié du plus ancien au plus récent, limité aux
 * 12 derniers mois - même logique que [GetPatrimoineHistoryUseCase], appliquée
 * à un actif individuel plutôt qu'au patrimoine global.
 */
class GetAssetValuationHistoryUseCase @Inject constructor(
    private val repository: AssetValuationSnapshotRepository
) {
    operator fun invoke(assetType: AssetValuationType, assetId: Long): Flow<List<AssetValuationSnapshot>> =
        repository.getForAsset(assetType, assetId).map { snapshots ->
            snapshots
                .groupBy { it.snapshotDate.withDayOfMonth(1) }
                .mapValues { (_, monthly) -> monthly.maxBy { it.snapshotDate } }
                .entries
                .sortedBy { it.key }
                .map { it.value }
                .takeLast(12)
        }
}
