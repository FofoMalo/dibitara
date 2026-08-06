package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.io.InputStream
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Parse un export CSV TradeRepublic et retourne une liste de [ImportedTransaction].
 *
 * Format attendu : RFC 4180, toutes les valeurs entre guillemets doubles, séparateur virgule.
 * 23 colonnes fixes - les index utilisés sont définis dans les constantes COL_*.
 *
 * Règles de mapping :
 * - TRADING/BUY       → EXPENSE + INVESTISSEMENT
 * - CARD_TRANSACTION  → EXPENSE + catégorie déduite du code MCC
 * - *_INBOUND         → INCOME  + AUTRE
 * - INSTANT_OUTBOUND  → EXPENSE + TRANSFERTS
 * - INTEREST_PAYMENT  → INCOME  + EPARGNE
 * - CARD_ORDERING_FEE → EXPENSE + AUTRE (montant dans la colonne fee, pas amount)
 */
object TradeRepublicCsvParser {

    // ─── Index des colonnes CSV TradeRepublic ──────────────────────────────────
    private const val COL_DATE             = 1
    private const val COL_CATEGORY         = 3   // "CASH" ou "TRADING"
    private const val COL_TYPE             = 4   // "CARD_TRANSACTION", "BUY", etc.
    private const val COL_NAME             = 6   // libellé marchand ou ETF
    private const val COL_AMOUNT           = 10  // montant principal (positif = crédit)
    private const val COL_FEE              = 11  // frais (utilisé si amount == 0)
    private const val COL_CURRENCY         = 13
    private const val COL_DESCRIPTION      = 17  // note saisie par l'utilisateur ou libellé TR
    private const val COL_TRANSACTION_ID   = 18  // UUID - clé de déduplication
    private const val COL_COUNTERPARTY     = 19  // nom de la contrepartie pour les virements
    private const val COL_MCC_CODE         = 22  // code MCC pour la catégorisation des paiements carte

    // ─── Tables de mapping MCC → catégorie Dibitara ───────────────────────────
    private val MCC_ALIMENTATION = setOf(5411, 5412, 5422, 5441, 5451, 5462, 5499, 5812, 5813, 5814)
    private val MCC_TRANSPORT    = setOf(4111, 4112, 4121, 4131, 5533, 5541, 5542, 5571)
    private val MCC_SANTE        = setOf(5047, 5122, 5912, 8011, 8021, 8049, 8062, 8099)
    private val MCC_LOISIRS      = setOf(5941, 5942, 5945, 7011, 7832, 7922, 7993, 7999)
    private val MCC_HABILLEMENT  = setOf(5311, 5621, 5651, 5661, 5699)
    private val MCC_ABONNEMENTS  = setOf(5045, 5065, 5734, 7372)

    // ─── Point d'entrée ───────────────────────────────────────────────────────

    /**
     * Lit le flux CSV et retourne les transactions parsées.
     * Les lignes invalides (colonnes manquantes, montant zéro, id vide) sont silencieusement ignorées.
     */
    fun parse(inputStream: InputStream): List<ImportedTransaction> {
        val lignes = inputStream.bufferedReader(Charsets.UTF_8).readLines()
        if (lignes.size < 2) return emptyList()
        // La première ligne est l'en-tête
        return lignes.drop(1).mapNotNull { ligne ->
            if (ligne.isBlank()) null else parseLigne(ligne)
        }
    }

    // ─── Parsing d'une ligne ──────────────────────────────────────────────────

    private fun parseLigne(ligne: String): ImportedTransaction? {
        val champs = parseCsvRow(ligne)
        if (champs.size < 23) return null

        val externalId = champs[COL_TRANSACTION_ID]
        if (externalId.isBlank()) return null

        val trType     = champs[COL_TYPE]
        val trCategory = champs[COL_CATEGORY]

        // Pour CARD_ORDERING_FEE, le montant réel est dans fee (amount = 0)
        val montantBrut  = parseDouble(champs[COL_AMOUNT])
        val feeBrut      = parseDouble(champs[COL_FEE])
        val montantEffectif = if (abs(montantBrut) > 0.001) montantBrut else feeBrut

        // Ignorer les lignes sans valeur réelle
        if (abs(montantEffectif) < 0.001) return null

        val amountCents = abs((montantEffectif * 100).roundToLong())
        val date        = parseDate(champs[COL_DATE]) ?: return null
        val devise      = parseCurrency(champs[COL_CURRENCY])

        // Les achats TRADING sont toujours des dépenses (débit du compte)
        val type = when {
            trCategory == "TRADING"   -> TransactionType.EXPENSE
            montantEffectif >= 0      -> TransactionType.INCOME
            else                      -> TransactionType.EXPENSE
        }

        return ImportedTransaction(
            date          = date,
            amountCents   = amountCents,
            currency      = devise,
            category      = determinerCategorie(trType, trCategory, champs),
            type          = type,
            note          = determinerNote(trType, trCategory, champs),
            externalId    = externalId,
            rawType       = trType,
            importSource  = "trade_republic"
        )
    }

    // ─── Catégorisation automatique ───────────────────────────────────────────

    private fun determinerCategorie(trType: String, trCategory: String, champs: List<String>): Category {
        if (trCategory == "TRADING") return Category.INVESTISSEMENT

        return when (trType) {
            "CUSTOMER_INBOUND",
            "TRANSFER_INBOUND",
            "TRANSFER_INSTANT_INBOUND"  -> Category.AUTRE

            "TRANSFER_INSTANT_OUTBOUND" -> Category.TRANSFERTS

            "INTEREST_PAYMENT"          -> Category.EPARGNE

            "CARD_ORDERING_FEE"         -> Category.AUTRE

            "CARD_TRANSACTION" -> {
                val mcc = champs[COL_MCC_CODE].toIntOrNull() ?: return Category.AUTRE
                when (mcc) {
                    in MCC_ALIMENTATION -> Category.ALIMENTATION
                    in MCC_TRANSPORT    -> Category.TRANSPORT
                    in MCC_SANTE        -> Category.SANTE
                    in MCC_LOISIRS      -> Category.LOISIRS
                    in MCC_HABILLEMENT  -> Category.HABILLEMENT
                    in MCC_ABONNEMENTS  -> Category.ABONNEMENTS
                    else                -> Category.AUTRE
                }
            }

            else -> Category.AUTRE
        }
    }

    // ─── Construction du libellé ──────────────────────────────────────────────

    private fun determinerNote(trType: String, trCategory: String, champs: List<String>): String {
        val name        = champs[COL_NAME].trim()
        val description = champs[COL_DESCRIPTION].trim()
        val contrepartie = champs[COL_COUNTERPARTY].trim()

        return when {
            trCategory == "TRADING"                -> name.ifBlank { "Achat ETF" }
            trType == "CARD_TRANSACTION"           -> name.ifBlank { "Paiement carte TR" }
            trType == "CARD_ORDERING_FEE"          -> "Frais carte TradeRepublic"
            trType == "INTEREST_PAYMENT"           -> "Intérêts TradeRepublic"
            trType == "CUSTOMER_INBOUND"           -> contrepartie.ifBlank { description }.ifBlank { "Virement reçu" }
            trType.startsWith("TRANSFER_")         -> description.ifBlank { "Virement TradeRepublic" }
            else                                   -> description.ifBlank { name }.ifBlank { "Transaction TradeRepublic" }
        }
    }

    // ─── Utilitaires de parsing ────────────────────────────────────────────────

    private fun parseDouble(s: String): Double = s.trim().toDoubleOrNull() ?: 0.0

    private fun parseDate(s: String): LocalDate? =
        runCatching { LocalDate.parse(s.trim()) }.getOrNull()

    private fun parseCurrency(s: String): Currency = when (s.trim().uppercase()) {
        "USD" -> Currency.USD
        "XOF" -> Currency.XOF
        "XAF" -> Currency.XAF
        else  -> Currency.EUR
    }

    /**
     * Parseur CSV conforme RFC 4180 : gère les valeurs entre guillemets et les guillemets doublés ("").
     * Nécessaire car les libellés peuvent contenir des virgules (ex : "Core MSCI World USD (Acc)").
     */
    private fun parseCsvRow(ligne: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < ligne.length) {
            if (ligne[i] == '"') {
                val sb = StringBuilder()
                i++ // saute le guillemet ouvrant
                while (i < ligne.length) {
                    when {
                        // guillemet doublé ("") → guillemet littéral dans la valeur
                        ligne[i] == '"' && i + 1 < ligne.length && ligne[i + 1] == '"' -> {
                            sb.append('"'); i += 2
                        }
                        ligne[i] == '"' -> { i++; break } // guillemet fermant
                        else            -> { sb.append(ligne[i]); i++ }
                    }
                }
                result.add(sb.toString())
                if (i < ligne.length && ligne[i] == ',') i++
            } else {
                val fin = ligne.indexOf(',', i).takeIf { it >= 0 } ?: ligne.length
                result.add(ligne.substring(i, fin))
                i = if (fin < ligne.length) fin + 1 else fin
            }
        }
        return result
    }
}
