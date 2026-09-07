package com.dibitara.app.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dibitara.app.data.importcsv.TradeRepublicNotificationParser
import com.dibitara.app.domain.usecase.CapturerTransactionLiveUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PACKAGE_TRADE_REPUBLIC = "de.traderepublic.app"
private const val TAG = "TrNotifListener"

/**
 * Capture en direct les paiements carte TradeRepublic via les notifications push de l'app
 * officielle. TradeRepublic ne pousse pas de notification pour les virements/versements
 * programmés : cette capture reste partielle, complémentaire à l'import CSV
 * (voir [TradeRepublicNotificationParser]).
 *
 * Même principe que [BredNotificationListenerService] : accès aux notifications à activer
 * manuellement dans les réglages Android (Paramètres → ACTION_NOTIFICATION_LISTENER_SETTINGS),
 * impossible à demander via une permission runtime classique.
 */
@AndroidEntryPoint
class TradeRepublicNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var capturerTransactionLive: CapturerTransactionLiveUseCase
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != PACKAGE_TRADE_REPUBLIC) {
            Log.d(TAG, "Notification ignorée, package : ${sbn.packageName}")
            return
        }

        val texte = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (texte == null) {
            Log.d(TAG, "Notification TradeRepublic reçue sans EXTRA_TEXT")
            return
        }

        val transaction = TradeRepublicNotificationParser.parse(texte)
        if (transaction == null) {
            Log.d(TAG, "Notification TradeRepublic reçue mais non reconnue par le parseur : \"$texte\"")
            return
        }

        scope.launch {
            val insere = capturerTransactionLive(transaction)
            if (insere) {
                notificationHelper.envoyerConfirmationCaptureLive(transaction.amountCents, transaction.note)
            }
        }
    }
}
