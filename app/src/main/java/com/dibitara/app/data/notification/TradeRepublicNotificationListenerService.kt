package com.dibitara.app.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dibitara.app.BuildConfig
import com.dibitara.app.data.importcsv.TradeRepublicNotificationParser
import com.dibitara.app.domain.usecase.CapturerTransactionLiveUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

private const val PACKAGE_TRADE_REPUBLIC = "de.traderepublic.app"
private const val TAG = "TrNotifListener"

// Fichier de diagnostic (même principe que BredNotificationListenerService) : logcat ne survit
// pas à une déconnexion adb, donc on trace aussi dans le stockage privé de l'app pour pouvoir
// relire l'historique quand le téléphone est reconnecté, même des jours après. Utile tant que
// la capture TradeRepublic n'a pas été confirmée en prod.
private const val NOM_FICHIER_DEBUG = "trade_republic_notif_debug.log"

/**
 * Capture en direct les paiements carte TradeRepublic via les notifications push de l'app
 * officielle. TradeRepublic ne pousse pas de notification pour les virements/versements
 * programmés : cette capture reste partielle, complémentaire à l'import CSV
 * (voir [TradeRepublicNotificationParser]).
 *
 * Même principe que [BredNotificationListenerService] : accès aux notifications à activer
 * manuellement dans les réglages Android (Paramètres → ACTION_NOTIFICATION_LISTENER_SETTINGS),
 * impossible à demander via une permission runtime classique. **Chaque listener se coche
 * séparément** : activer celui de BRED n'active pas celui-ci (piège constaté le 2026-09-08 -
 * l'écran Paramètres affiche désormais l'état des deux, voir SettingsViewModel.captureLiveState).
 */
@AndroidEntryPoint
class TradeRepublicNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var capturerTransactionLive: CapturerTransactionLiveUseCase
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != PACKAGE_TRADE_REPUBLIC) {
            // Garde-fou : si le package fourni s'avère incorrect, ce log permet de le corriger
            // sans redéployer une version de debug dédiée.
            Log.d(TAG, "Notification ignorée, package : ${sbn.packageName}")
            return
        }

        // On ne sait pas avec certitude quel champ TradeRepublic remplit ("Dépensé X € à …"
        // peut être le titre, le corps ou le corps déplié) : on les concatène tous, séparés
        // par un retour à la ligne pour que la capture du marchand par le parseur (« . » ne
        // matche pas « \n ») s'arrête au bon endroit.
        val extras = sbn.notification.extras
        val texte = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        ).joinToString("\n").trim()

        if (texte.isEmpty()) {
            Log.d(TAG, "Notification TradeRepublic reçue sans texte exploitable")
            ecrireLogDebug("SANS TEXTE : title/text/bigText tous absents")
            return
        }

        val transaction = TradeRepublicNotificationParser.parse(texte)
        if (transaction == null) {
            Log.d(TAG, "Notification TradeRepublic reçue mais non reconnue par le parseur : \"$texte\"")
            ecrireLogDebug("NON RECONNUE : \"$texte\"")
            return
        }

        ecrireLogDebug("RECONNUE : ${transaction.amountCents} centimes, ${transaction.note}")
        scope.launch {
            val insere = capturerTransactionLive(transaction)
            ecrireLogDebug(if (insere) "INSÉRÉE" else "DOUBLON ignoré")
            if (insere) {
                notificationHelper.envoyerConfirmationCaptureLive(transaction.amountCents, transaction.note)
            }
        }
    }

    /**
     * Trace chaque notification TradeRepublic reçue dans un fichier privé à l'app, lisible même
     * sans connexion adb au moment de l'événement (voir commentaire sur [NOM_FICHIER_DEBUG]).
     */
    private fun ecrireLogDebug(message: String) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            File(filesDir, NOM_FICHIER_DEBUG).appendText("${LocalDateTime.now()} $message\n")
        }
    }
}
