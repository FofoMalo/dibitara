package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
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
 * TradeRepublic ne pousse pas de notification pour les virements/versements programmés :
 * cette capture reste partielle, complémentaire à l'import CSV mensuel (TradeRepublicCsvParser).
 */
internal object TradeRepublicNotificationParser {

    private val REGEX_DEPENSE = Regex(
        """Dépensé\s+([\d,]+)\s*€\s*à\s*(.+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(texteNotification: String): ImportedTransaction? {
        val match = REGEX_DEPENSE.find(texteNotification) ?: return null
        val (montantStr, marchandBrut) = match.destructured

        val amountCents = parseMontantCents(montantStr) ?: return null
        val date = LocalDate.now()
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
            importSource = "trade_republic_notification"
        )
    }

    // roundToLong() plutôt que toLong() : évite qu'une imprécision binaire double
    // (ex. 28.45 * 100 = 2844.9999999999995) tronque le montant d'un centime.
    private fun parseMontantCents(montantStr: String): Long? =
        montantStr.replace(",", ".").toDoubleOrNull()?.let { (it * 100).roundToLong() }
}
