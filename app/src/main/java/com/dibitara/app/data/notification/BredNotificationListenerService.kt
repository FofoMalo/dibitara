package com.dibitara.app.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dibitara.app.data.importcsv.BredNotificationParser
import com.dibitara.app.domain.usecase.CapturerTransactionLiveUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PACKAGE_BRED = "fr.bred.fr"
private const val TAG = "BredNotifListener"

/**
 * Capture en direct les paiements carte BRED via les notifications push de l'app officielle.
 * BRED ne pousse pas de notification pour les virements/prélèvements/retraits DAB : cette
 * capture reste partielle, complémentaire à l'import CSV mensuel (voir [BredNotificationParser]).
 *
 * L'utilisateur doit activer manuellement l'accès aux notifications dans les réglages Android
 * (bouton dédié dans Paramètres → ACTION_NOTIFICATION_LISTENER_SETTINGS) : impossible à demander
 * via une permission runtime classique, contrairement à POST_NOTIFICATIONS.
 */
@AndroidEntryPoint
class BredNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var capturerTransactionLive: CapturerTransactionLiveUseCase
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != PACKAGE_BRED) {
            // Garde-fou : si le package fourni s'avère incorrect, ce log permet de le corriger
            // sans avoir à redéployer une version de debug dédiée.
            Log.d(TAG, "Notification ignorée, package : ${sbn.packageName}")
            return
        }

        val texte = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val transaction = BredNotificationParser.parse(texte) ?: return

        scope.launch {
            val insere = capturerTransactionLive(transaction)
            if (insere) {
                notificationHelper.envoyerConfirmationCaptureLive(transaction.amountCents, transaction.note)
            }
        }
    }
}
