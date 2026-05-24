package com.dibitara.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Worker périodique qui envoie un résumé du mois précédent en notification.
 *
 * Planifié par [SettingsViewModel] quand l'utilisateur active les notifications mensuelles.
 * Tourne au début du mois pour présenter le bilan du mois écoulé.
 *
 * @HiltWorker : permet l'injection de dépendances via Hilt.
 * @AssistedInject : Hilt exige ce pattern pour les Workers (context + params sont "assistés").
 */
@HiltWorker
class MonthlyReportNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getMonthlyReport: GetMonthlyReportUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Le worker tourne en début de mois → on récupère le rapport du mois précédent
        val moisPrecedent = LocalDate.now().minusMonths(1)
        val rapport = getMonthlyReport(moisPrecedent.monthValue, moisPrecedent.year).first()
        if (rapport != null) {
            notificationHelper.envoyerResumeMensuel(rapport)
        }
        return Result.success()
    }

    companion object {
        const val NOM_TRAVAIL_UNIQUE = "monthly_report_notification"
    }
}
