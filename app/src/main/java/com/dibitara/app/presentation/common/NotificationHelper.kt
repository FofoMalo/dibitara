package com.dibitara.app.presentation.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilitaire centralisé pour toutes les notifications de l'application.
 * Crée les canaux au démarrage (requis Android 8+) et expose des méthodes
 * métier claires pour chaque type d'alerte.
 *
 * @Singleton : une seule instance partagée dans toute l'application,
 * garantit que les canaux ne sont créés qu'une fois.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CANAL_BUDGET = "canal_budget"
        const val CANAL_DETTES = "canal_dettes"
        const val CANAL_FONDS  = "canal_fonds"

        // Identifiants uniques pour chaque notification (évite les doublons)
        private const val NOTIF_ID_BUDGET = 1001
        private const val NOTIF_ID_FONDS  = 3001
    }

    init {
        creerCanaux()
    }

    // ─── Création des canaux ──────────────────────────────────────────────────

    private fun creerCanaux() {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CANAL_BUDGET, "Budget", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alertes quand le budget mensuel est dépassé" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_DETTES, "Dettes", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Rappels d'échéances de remboursement" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_FONDS, "Fonds disponibles", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Avertissements quand le solde est bas" }
        )
    }

    // ─── Méthodes d'envoi ─────────────────────────────────────────────────────

    /**
     * Alerte quand les dépenses du mois dépassent le budget alloué.
     */
    fun envoyerAlerteBudget(depenseCents: Long, alloueCents: Long) {
        val notification = NotificationCompat.Builder(context, CANAL_BUDGET)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Budget dépassé")
            .setContentText(
                "Vous avez dépensé ${depenseCents / 100}€ " +
                "sur ${alloueCents / 100}€ alloués ce mois-ci."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        envoyerSiAutorise(NOTIF_ID_BUDGET, notification)
    }

    /**
     * Rappel pour une échéance de dette arrivant aujourd'hui.
     * L'ID de notification est dérivé de l'ID de la dette pour éviter
     * qu'une dette écrase la notification d'une autre.
     */
    fun envoyerRappelDette(idDette: Long, labelDette: String, montantCents: Long) {
        val notification = NotificationCompat.Builder(context, CANAL_DETTES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Échéance dette aujourd'hui")
            .setContentText("Paiement de ${montantCents / 100}€ prévu : $labelDette")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // 2000 + idDette garantit un ID unique par dette (pas de collision avec les autres types)
        envoyerSiAutorise((2000 + idDette).toInt(), notification)
    }

    /**
     * Avertissement quand le solde estimé du mois passe sous le seuil configuré.
     */
    fun envoyerAvertissementFonds(soldeCents: Long, seuilCents: Long) {
        val notification = NotificationCompat.Builder(context, CANAL_FONDS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Liquidités insuffisantes")
            .setContentText(
                "Solde estimé : ${soldeCents / 100}€ " +
                "(seuil d'alerte : ${seuilCents / 100}€)"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        envoyerSiAutorise(NOTIF_ID_FONDS, notification)
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    /**
     * Vérifie que l'utilisateur a accordé la permission POST_NOTIFICATIONS
     * avant d'envoyer (obligatoire Android 13+, évite un crash SecurityException).
     */
    private fun envoyerSiAutorise(id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(id, notification)
        }
    }
}
