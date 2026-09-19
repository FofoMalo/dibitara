package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.TradeRepublicReconciliation
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Parse le texte d'une notification push TradeRepublic (paiement carte) pour en extraire une
 * dépense en direct.
 *
 * Format connu et confirmé (capture d'écran réelle, 2026-09-07) :
 * - "Dépensé 28,45 € à FRANPRIX"
 *
 * Contrairement à [BredNotificationParser], le texte ne porte pas de date : on retient la date
 * du jour ([LocalDate.now]), la notification étant reçue en temps quasi réel à chaque paiement.
 *
 * Roundup et plans exécutés : formats confirmés par la capture du 09/09/2026.
 * Les virements attendent un exemple réel avant d’être reconnus.
 */
internal object TradeRepublicNotificationParser {

    // Espaces insécables du formatage monétaire français : U+00A0 (insécable) placée avant le
    // « € », et U+202F (fine insécable) utilisée par certaines locales comme séparateur de
    // milliers. Le \s des Regex Java est purement ASCII et n'en reconnaît aucune : sans les
    // normaliser, un texte parfaitement correct à l'écran échoue silencieusement.
    private const val ESPACE_INSECABLE = '\u00A0'
    private const val ESPACE_FINE_INSECABLE = '\u202F'

    // Le montant tolère une espace interne (séparateur de milliers), retirée avant conversion
    // dans parseMontantCents. Le marchand est capturé jusqu'à la fin de ligne (« . » ne matche
    // pas « \n ») : si le service concatène plusieurs champs sur des lignes distinctes, seul le
    // segment « Dépensé X € à MARCHAND » est retenu.
    private val REGEX_DEPENSE = Regex(
        """Dépensé\s+([\d ,]+)\s*€\s*à\s*(.+)""",
        RegexOption.IGNORE_CASE
    )

    private const val MONTANT = """(\d{1,3}(?: \d{3})*,\d{2}|\d+,\d{2})"""
    private val REGEX_PLAN = Regex(
        """^Votre plan d'épargne sur (.+?) de $MONTANT\s*€ a été exécuté[.!]?$""",
        RegexOption.IGNORE_CASE
    )
    private val REGEX_ROUNDUP = Regex(
        """^Vous avez économisé et investi $MONTANT\s*€ dans le Round up\s*!$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        texteNotification: String,
        date: LocalDate = LocalDate.now(),
        notificationId: String? = null
    ): ImportedTransaction? {
        // On ramène les espaces insécables à une espace ordinaire avant tout matching (même
        // principe que la normalisation d'apostrophe dans BredNotificationParser).
        val texte = texteNotification
            .replace(ESPACE_INSECABLE, ' ')
            .replace(ESPACE_FINE_INSECABLE, ' ')
            .replace('’', '\'')

        // Chaque champ Android est analysé séparément. Le corps développé peut être multiligne.
        val corps = texte.trim().replace(Regex("\\s+"), " ")
        val plan = REGEX_PLAN.matchEntire(corps)
        val roundup = REGEX_ROUNDUP.matchEntire(corps)
        if (plan != null || roundup != null) {
            val montant = plan?.groupValues?.get(2) ?: roundup!!.groupValues[1]
            val cents = runCatching {
                montant.replace(" ", "").replace(",", ".").toBigDecimal()
                    .movePointRight(2).longValueExact()
            }.getOrNull()?.takeIf { it > 0 } ?: return null
            val nature = if (plan != null) "SAVINGS_PLAN_NOTIF" else "ROUNDUP_NOTIF"
            val note = if (plan != null) "Plan d’épargne · ${plan.groupValues[1].trim()}" else "Roundup investi"
            return ImportedTransaction(
                date = date, amountCents = cents, currency = Currency.EUR,
                category = Category.INVESTISSEMENT, type = TransactionType.EXPENSE,
                note = note,
                // L’identité Android distingue deux exécutions identiques le même jour.
                externalId = BredCategoriseur.genererExternalId(
                    "trade_republic_notification", date, "$nature|${notificationId ?: note}", cents
                ),
                rawType = nature, importSource = "trade_republic_notification",
                reconciliationKey = if (plan != null) TradeRepublicReconciliation.plan(plan.groupValues[1]) else "roundup"
            )
        }

        val match = REGEX_DEPENSE.find(texte) ?: return null
        val (montantStr, marchandBrut) = match.destructured

        val amountCents = parseMontantCents(montantStr) ?: return null
        val marchand = marchandBrut.trim()
        // BredCategoriseur est un classifieur générique par mots-clés (noms de marchands
        // français), pas spécifique à la banque BRED - réutilisé ici pour éviter de dupliquer
        // les listes de mots-clés.
        val marchandNormalise = BredCategoriseur.normaliser(marchand)

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = Currency.EUR,
            category     = BredCategoriseur.verifierMotsCles(marchandNormalise) ?: Category.AUTRE,
            type         = TransactionType.EXPENSE,
            note         = marchand,
            // Le marchand est inclus dans la clé (contrairement aux captures BRED) : les
            // paiements carte TradeRepublic sont le flux de dépense quotidien, pas un cas rare -
            // deux achats du même montant le même jour à des marchands différents ne doivent pas
            // être confondus (voir BredCategoriseur.genererExternalId vs genererExternalIdMontantDate).
            externalId   = BredCategoriseur.genererExternalId("trade_republic_notification", date, marchand, amountCents),
            rawType      = "CARD_TRANSACTION_NOTIF",
            importSource = "trade_republic_notification",
            reconciliationKey = TradeRepublicReconciliation.carte(marchand)
        )
    }

    // roundToLong() plutôt que toLong() : évite qu'une imprécision binaire double
    // (ex. 28.45 * 100 = 2844.9999999999995) tronque le montant d'un centime.
    // replace(" ", "") retire le séparateur de milliers éventuel avant la conversion.
    private fun parseMontantCents(montantStr: String): Long? =
        montantStr.replace(" ", "").replace(",", ".").toDoubleOrNull()?.let { (it * 100).roundToLong() }
}
