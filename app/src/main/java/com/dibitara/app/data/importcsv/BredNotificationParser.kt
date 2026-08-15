package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * Parse le texte d'une notification push BRED pour en extraire un paiement carte ou un retrait.
 *
 * Formats connus et confirmés (captures d'écran / logs réels) :
 * - "La BRED vous confirme votre paiement carte d'un montant de 35,30€ (BAR LES ARCADES) le 03/08/2026."
 * - "La BRED vous confirme votre retrait carte  d'un montant de 40,00€ le 15/08/2026."
 *   (apostrophe et double espace variables selon les messages BRED, d'où le regex tolérant ci-dessous)
 *
 * BRED ne pousse pas de notification pour les virements/prélèvements :
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

    // ['’] : BRED utilise tantôt l'apostrophe droite, tantôt l'apostrophe typographique
    // selon les messages. \s+ plutôt qu'un espace unique : le message réel contient un
    // double espace entre "carte" et "d'un" ("retrait carte  d'un montant...").
    private val REGEX_RETRAIT = Regex(
        """retrait carte\s+d['’]un montant de ([\d,]+)\s*€\s*le\s*(\d{2}/\d{2}/\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun parse(texteNotification: String): ImportedTransaction? {
        parsePaiementCarte(texteNotification)?.let { return it }
        return parseRetrait(texteNotification)
    }

    private fun parsePaiementCarte(texteNotification: String): ImportedTransaction? {
        val match = REGEX_PAIEMENT_CARTE.find(texteNotification) ?: return null
        val (montantStr, marchandBrut, dateStr) = match.destructured

        val amountCents = parseMontantCents(montantStr) ?: return null
        val date = parseDate(dateStr) ?: return null
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

    private fun parseRetrait(texteNotification: String): ImportedTransaction? {
        val match = REGEX_RETRAIT.find(texteNotification) ?: return null
        val (montantStr, dateStr) = match.destructured

        val amountCents = parseMontantCents(montantStr) ?: return null
        val date = parseDate(dateStr) ?: return null

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = Currency.EUR,
            category     = Category.AUTRE, // même catégorisation que les retraits importés en CSV
            type         = TransactionType.EXPENSE,
            note         = "Retrait",
            externalId   = BredCategoriseur.genererExternalIdMontantDate("bred_notification", date, amountCents),
            rawType      = "RETRAIT_NOTIF",
            importSource = "bred_notification"
        )
    }

    // roundToLong() plutôt que toLong() : évite qu'une imprécision binaire double
    // (ex. 35.30 * 100 = 3529.9999999999995) tronque le montant d'un centime.
    private fun parseMontantCents(montantStr: String): Long? =
        montantStr.replace(",", ".").toDoubleOrNull()?.let { (it * 100).roundToLong() }

    private fun parseDate(dateStr: String): LocalDate? =
        runCatching { LocalDate.parse(dateStr, DATE_FORMAT) }.getOrNull()
}
