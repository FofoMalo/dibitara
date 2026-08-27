package com.dibitara.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.SavePatrimoineSnapshotUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Worker périodique (30 jours) qui garantit au moins un snapshot du patrimoine par mois,
 * même si l'utilisateur n'ouvre jamais l'écran "Détail du patrimoine" (seul autre déclencheur,
 * voir [com.dibitara.app.presentation.patrimoine.PatrimoineDetailViewModel.init] - opportuniste,
 * pas garanti).
 *
 * [SavePatrimoineSnapshotUseCase] déduplique déjà par jour (un seul snapshot par date) : ce
 * worker peut donc tourner sans risque même un jour où un snapshot a déjà été enregistré
 * manuellement en ouvrant l'écran.
 *
 * Planifié inconditionnellement au démarrage par [com.dibitara.app.presentation.AppViewModel.init]
 * - contrairement aux notifications, l'historique du patrimoine n'est pas une préférence
 * activable/désactivable.
 */
@HiltWorker
class PatrimoineSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getPatrimonyOverview: GetPatrimonyOverviewUseCase,
    private val saveSnapshot: SavePatrimoineSnapshotUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val overview = getPatrimonyOverview(today.monthValue, today.year).first()
        saveSnapshot(overview)
        return Result.success()
    }

    companion object {
        const val NOM_TRAVAIL_UNIQUE = "patrimoine_snapshot_mensuel"
    }
}
