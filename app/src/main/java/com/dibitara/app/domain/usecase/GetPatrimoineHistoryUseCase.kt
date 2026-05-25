package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retourne l'historique mensuel du patrimoine : un point par mois (le dernier
 * snapshot du mois), trié du plus ancien au plus récent, limité aux 12 derniers mois.
 *
 * Regrouper par mois évite plusieurs points par mois si l'utilisateur ouvre l'app
 * plusieurs jours de suite.
 */
class GetPatrimoineHistoryUseCase @Inject constructor(
    private val repository: PatrimoineSnapshotRepository
) {
    operator fun invoke(): Flow<List<PatrimoineSnapshot>> =
        repository.getAll().map { snapshots ->
            snapshots
                // Un point par mois : on garde le dernier snapshot de chaque mois
                .groupBy { it.snapshotDate.withDayOfMonth(1) }
                .mapValues { (_, monthly) -> monthly.maxBy { it.snapshotDate } }
                .entries
                .sortedBy { it.key }
                .map { it.value }
                .takeLast(12)
        }
}
