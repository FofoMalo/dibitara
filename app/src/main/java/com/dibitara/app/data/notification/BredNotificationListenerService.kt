package com.dibitara.app.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dibitara.app.BuildConfig
import com.dibitara.app.data.importcsv.BredNotificationParser
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

private const val PACKAGE_BRED = "fr.bred.fr"
private const val TAG = "BredNotifListener"

// Fichier de diagnostic temporaire (chantier "capture live BRED ne capture rien", 2026-08-13) :
// logcat ne survit pas à une déconnexion adb, donc on trace aussi dans le stockage privé de
// l'app pour pouvoir relire l'historique quand le téléphone est reconnecté, même des jours après.
// À retirer une fois la cause du problème de capture confirmée et corrigée.
private const val NOM_FICHIER_DEBUG = "bred_notif_debug.log"

/**
 * Capture en direct les paiements carte et les retraits BRED via les notifications push de
 * l'app officielle. BRED ne pousse pas de notification pour les virements/prélèvements : cette
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

        val texte = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (texte == null) {
            Log.d(TAG, "Notification BRED reçue sans EXTRA_TEXT")
            ecrireLogDebug("Notification BRED reçue sans EXTRA_TEXT")
            return
        }

        val transaction = BredNotificationParser.parse(texte)
        if (transaction == null) {
            // Texte affiché uniquement en debug : contient le montant et le marchand.
            Log.d(TAG, "Notification BRED reçue mais non reconnue par le parseur : \"$texte\"")
            ecrireLogDebug("NON RECONNUE : \"$texte\"")
            return
        }

        ecrireLogDebug("RECONNUE : ${transaction.amountCents} centimes, ${transaction.note}")
        scope.launch {
            val insere = capturerTransactionLive(transaction)
            if (insere) {
                notificationHelper.envoyerConfirmationCaptureLive(transaction.amountCents, transaction.note)
            }
        }
    }

    /**
     * Trace chaque notification BRED reçue dans un fichier privé à l'app, lisible même sans
     * connexion adb au moment de l'événement (voir commentaire sur [NOM_FICHIER_DEBUG]).
     */
    private fun ecrireLogDebug(message: String) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            File(filesDir, NOM_FICHIER_DEBUG).appendText("${LocalDateTime.now()} $message\n")
        }
    }
}
