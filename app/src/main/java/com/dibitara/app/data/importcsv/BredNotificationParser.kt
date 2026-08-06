package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * Parse le texte d'une notification push BRED pour en extraire un paiement carte.
 *
 * Seul format connu et confirmé (capture d'écran réelle) :
 * "La BRED vous confirme votre paiement carte d'un montant de 35,30€ (BAR LES ARCADES) le 03/08/2026."
 *
 * BRED ne pousse pas de notification pour les virements/prélèvements/retraits DAB :
 * cette capture reste partielle, complémentaire à l'import CSV mensuel (BredCsvParser).
 */
internal object BredNotificationParser {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Le marchand est capturé de façon non-gourmande jusqu'à la première parenthèse fermante,
    // pour ignorer le reste du texte (ex. "contactez Bred Direct au ... (service gratuit...)").
    private val REGEX_PAIEMENT_CARTE = Regex(
        """paiement carte d'un montant de ([\d,]+)\s*€\s*\(([^)]+)\)\s*le\s*(\d{2}/\d{2}/\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun parse(texteNotification: String): ImportedTransaction? {
        val match = REGEX_PAIEMENT_CARTE.find(texteNotification) ?: return null
        val (montantStr, marchandBrut, dateStr) = match.destructured

        // roundToLong() plutôt que toLong() : évite qu'une imprécision binaire double
        // (ex. 35.30 * 100 = 3529.9999999999995) tronque le montant d'un centime.
        val amountCents = montantStr.replace(",", ".").toDoubleOrNull()
            ?.let { (it * 100).roundToLong() } ?: return null
        val date = runCatching { LocalDate.parse(dateStr, DATE_FORMAT) }.getOrNull() ?: return null
        val marchand = marchandBrut.trim()
        val marchandNormalise = BredCategoriseur.normaliser(marchand)

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = Currency.EUR,
            category     = BredCategoriseur.verifierMotsCles(marchandNormalise) ?: Category.AUTRE,
            type         = TransactionType.EXPENSE,
            note         = marchand,
            externalId   = BredCategoriseur.genererExternalIdMontantDate("bred_notification", date, amountCents),
            rawType      = "PAIEMENT_CARTE_NOTIF",
            importSource = "bred_notification"
        )
    }
}
