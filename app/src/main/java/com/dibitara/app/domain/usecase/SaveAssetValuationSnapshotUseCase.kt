package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Sauvegarde un instantané de la valeur d'un actif pour aujourd'hui, sauf si un
 * snapshot existe déjà pour ce jour et cet actif (pas de doublon intra-journalier).
 *
 * Appelé en fire-and-forget depuis InvestmentsViewModel après un ajout/une
 * modification de bien immobilier ou de SCPI, pour constituer un historique de
 * valorisation passif sans aucune saisie dédiée.
 */
class SaveAssetValuationSnapshotUseCase @Inject constructor(
    private val repository: AssetValuationSnapshotRepository
) {
    suspend operator fun invoke(
        assetType  : AssetValuationType,
        assetId    : Long,
        valueCents : Long,
        currency   : Currency
    ): Result<Unit> = runCatching {
        val today = LocalDate.now()
        if (!repository.existsForDay(assetType, assetId, today)) {
            repository.save(
                AssetValuationSnapshot(
                    assetType    = assetType,
                    assetId      = assetId,
                    snapshotDate = today,
                    valueCents   = valueCents,
                    currency     = currency
                )
            )
        }
    }
}
