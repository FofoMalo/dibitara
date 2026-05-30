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
 * Worker périodique (7 jours) qui envoie un mini-récap hebdomadaire du mois en cours.
 *
 * Réutilise [CANAL_MENSUEL] de [NotificationHelper] pour rester dans le même groupe
 * de notifications que le bilan mensuel — pas de canal supplémentaire nécessaire.
 *
 * Planifié par [SettingsViewModel.mettreAJourNotificationsMensuelles] avec
 * [ExistingPeriodicWorkPolicy.KEEP] pour ne pas remettre à zéro l'intervalle existant.
 */
@HiltWorker
class WeeklyRecapWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getMonthlyReport: GetMonthlyReportUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today  = LocalDate.now()
        val rapport = getMonthlyReport(today.monthValue, today.year).first()
            ?: return Result.success()

        val sym = rapport.currency.symbol
        // Mini-récap : dépenses de la semaine écoulée + solde du mois courant
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.CANAL_MENSUEL
        )
            .setSmallIcon(com.dibitara.app.R.drawable.ic_launcher_foreground)
            .setContentTitle("Récap hebdo")
            .setContentText(
                "Dépenses ce mois : ${rapport.depensesCents / 100}$sym  " +
                "· Solde : ${rapport.soldeCents / 100}$sym"
            )
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = androidx.core.app.NotificationManagerCompat.from(applicationContext)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIF_ID_WEEKLY, notification)
        }
        return Result.success()
    }

    companion object {
        const val NOM_TRAVAIL_UNIQUE = "weekly_recap_notification"
        private const val NOTIF_ID_WEEKLY = 6001
    }
}
