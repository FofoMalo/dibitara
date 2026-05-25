package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.PatrimoineSnapshot
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Sauvegarde un snapshot du patrimoine pour aujourd'hui, sauf si un snapshot
 * existe déjà pour ce jour (pas de doublon intra-journalier).
 *
 * Appelé en fire-and-forget depuis PatrimoineDetailViewModel.init
 * pour constituer un historique passif sans action de l'utilisateur.
 */
class SavePatrimoineSnapshotUseCase @Inject constructor(
    private val repository: PatrimoineSnapshotRepository
) {
    suspend operator fun invoke(overview: PatrimonyOverview): Result<Unit> = runCatching {
        val today = LocalDate.now()
        if (!repository.existsForDay(today)) {
            repository.save(
                PatrimoineSnapshot(
                    snapshotDate        = today,
                    patrimoineBrutCents = overview.patrimoineBrutCents,
                    patrimoineNetCents  = overview.patrimoineNetCents,
                    currency            = overview.currency
                )
            )
        }
    }
}
