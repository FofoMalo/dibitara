package com.dibitara.app.presentation.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dibitara.app.R
import com.dibitara.app.domain.model.MonthlyReport
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
        const val CANAL_BUDGET         = "canal_budget"
        const val CANAL_DETTES         = "canal_dettes"
        const val CANAL_FONDS          = "canal_fonds"
        const val CANAL_MENSUEL        = "canal_mensuel"
        const val CANAL_CONTRIBUTIONS  = "canal_contributions"

        private const val NOTIF_ID_BUDGET         = 1001
        private const val NOTIF_ID_FONDS          = 3001
        private const val NOTIF_ID_MENSUEL        = 4001
        const val          NOTIF_ID_CONTRIBUTIONS = 5001
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
        manager.createNotificationChannel(
            NotificationChannel(CANAL_MENSUEL, "Bilan mensuel", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Résumé du mois écoulé envoyé en début de mois" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_CONTRIBUTIONS, "Versements à faire", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Rappel de versements mensuels épargne/SCPI non effectués" }
        )
    }

    // ─── Méthodes d'envoi ─────────────────────────────────────────────────────

    /**
     * Alerte quand les dépenses du mois dépassent le budget alloué.
     */
    fun envoyerAlerteBudget(depenseCents: Long, alloueCents: Long) {
        val notification = NotificationCompat.Builder(context, CANAL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
     * Un clic sur la notification ouvre directement l'écran Paramètres pour
     * que l'utilisateur puisse ajuster son seuil d'alerte.
     */
    fun envoyerAvertissementFonds(soldeCents: Long, seuilCents: Long) {
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("dibitara://settings")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_FONDS,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CANAL_FONDS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Liquidités insuffisantes")
            .setContentText(
                "Solde estimé : ${soldeCents / 100}€ " +
                "(seuil d'alerte : ${seuilCents / 100}€)"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        envoyerSiAutorise(NOTIF_ID_FONDS, notification)
    }

    /**
     * Résumé du mois écoulé : revenus, dépenses et solde en une ligne.
     * Appelé par [MonthlyReportNotificationWorker] en début de mois.
     */
    fun envoyerResumeMensuel(rapport: MonthlyReport) {
        val moisLabel = moisComplet(rapport.month)
        val sym = rapport.currency.symbol
        val notification = NotificationCompat.Builder(context, CANAL_MENSUEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bilan $moisLabel ${rapport.year}")
            .setContentText(
                "Revenus : ${rapport.revenusCents / 100}$sym  " +
                "· Dépenses : ${rapport.depensesCents / 100}$sym  " +
                "· Solde : ${rapport.soldeCents / 100}$sym"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        envoyerSiAutorise(NOTIF_ID_MENSUEL, notification)
    }

    /**
     * Rappel de fin de mois : N versements épargne/SCPI non encore effectués.
     * Affiché uniquement dans les 5 derniers jours du mois (décision du ViewModel).
     */
    fun envoyerRappelContributions(count: Int, totalCents: Long) {
        val notification = NotificationCompat.Builder(context, CANAL_CONTRIBUTIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Versements à faire")
            .setContentText("$count versement(s) à faire · Total : ${totalCents / 100}€")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        envoyerSiAutorise(NOTIF_ID_CONTRIBUTIONS, notification)
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    /**
     * Vérifie que l'utilisateur a accordé la permission POST_NOTIFICATIONS
     * avant d'envoyer (obligatoire Android 13+, évite un crash SecurityException).
     */
    private fun moisComplet(month: Int): String = when (month) {
        1 -> "janvier"; 2 -> "février"; 3 -> "mars"; 4 -> "avril"
        5 -> "mai"; 6 -> "juin"; 7 -> "juillet"; 8 -> "août"
        9 -> "septembre"; 10 -> "octobre"; 11 -> "novembre"; else -> "décembre"
    }

    private fun envoyerSiAutorise(id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(id, notification)
        }
    }
}
