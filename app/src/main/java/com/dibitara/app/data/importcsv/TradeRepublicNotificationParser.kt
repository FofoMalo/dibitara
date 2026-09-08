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

    fun parse(texteNotification: String): ImportedTransaction? {
        // On ramène les espaces insécables à une espace ordinaire avant tout matching (même
        // principe que la normalisation d'apostrophe dans BredNotificationParser).
        val texte = texteNotification
            .replace(ESPACE_INSECABLE, ' ')
            .replace(ESPACE_FINE_INSECABLE, ' ')

        val match = REGEX_DEPENSE.find(texte) ?: return null
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
    // replace(" ", "") retire le séparateur de milliers éventuel avant la conversion.
    private fun parseMontantCents(montantStr: String): Long? =
        montantStr.replace(" ", "").replace(",", ".").toDoubleOrNull()?.let { (it * 100).roundToLong() }
}
