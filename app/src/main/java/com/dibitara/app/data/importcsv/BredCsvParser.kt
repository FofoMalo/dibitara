package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Parse un export CSV BRED et retourne une liste de [ImportedTransaction].
 *
 * Format attendu (export "Télécharger les opérations" sur bred.fr) :
 * - Séparateur : point-virgule ; valeurs optionnellement entre guillemets doubles
 * - Encodage   : UTF-8 ou ISO-8859-1
 * - Date       : DD/MM/YYYY
 * - Montant    : notation française (virgule décimale, espace milliers)
 *                négatif = dépense, positif = revenu
 *
 * Trois variantes supportées :
 *   Format A - 4 colonnes  : Date ; Libellé ; Montant ; Devise
 *   Format B - 5 colonnes  : Date opération ; Date valeur ; Libellé ; Montant ; Devise
 *   Format C - 13 colonnes : export "historique des opérations" de bred.fr —
 *              Date de l'opération ; Référence ; Type de l'opération ; Catégorie ;
 *              Sous catégorie ; Montant ; Commentaire ; Détail 1..6
 *
 * La catégorisation et la génération d'externalId sont déléguées à [BredCategoriseur].
 */
object BredCsvParser {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // ─── Point d'entrée ───────────────────────────────────────────────────────

    /**
     * Lit le flux [inputStream] et retourne les transactions parsées.
     * Essaie UTF-8 puis ISO-8859-1 pour les exports anciens de bred.fr.
     */
    fun parse(inputStream: InputStream): List<ImportedTransaction> {
        val contenu = runCatching {
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        }.getOrElse {
            inputStream.bufferedReader(Charsets.ISO_8859_1).readText()
        }

        val lignes = contenu.lines().filter { it.isNotBlank() }
        if (lignes.size < 2) return emptyList()

        // Cherche la première ligne d'en-tête reconnaissable (contient "Date" ou "date")
        val indexEnTete = lignes.indexOfFirst { it.contains("Date", ignoreCase = true) }
        if (indexEnTete < 0 || indexEnTete >= lignes.size - 1) return emptyList()

        val colonnes = parseRow(lignes[indexEnTete])
        val donnees  = lignes.drop(indexEnTete + 1)

        // Format C : la 3e colonne de l'en-tête contient "Type" (ex. "Type de l'opération")
        // → colonnes fixes distinctes des formats A/B, voir parseLigneFormatC
        if (colonnes.size >= 6 && colonnes[2].contains("Type", ignoreCase = true)) {
            return donnees.mapNotNull { parseLigneFormatC(it) }
        }

        // Format B : la 2e colonne de l'en-tête contient "valeur" → décale les colonnes de 1
        val offsetLib = if (colonnes.size >= 5 && colonnes[1].contains("valeur", ignoreCase = true)) 1 else 0

        return donnees.mapNotNull { parseLigne(it, offsetLib) }
    }

    // ─── Parsing d'une ligne ──────────────────────────────────────────────────

    private fun parseLigne(ligne: String, offsetLib: Int): ImportedTransaction? {
        val c = parseRow(ligne)
        // Format A : min 4 colonnes ; Format B : min 5
        if (c.size < 4 + offsetLib) return null

        val date    = parseDate(c[0]) ?: return null
        val libelle = c[1 + offsetLib].trim()
        val montant = parseMontantFr(c[2 + offsetLib]) ?: return null
        if (abs(montant) < 0.001) return null

        val devise      = if (c.size > 3 + offsetLib) parseCurrency(c[3 + offsetLib]) else Currency.EUR
        // roundToLong() plutôt que toLong() : évite qu'une imprécision binaire double
        // (ex. 35.30 * 100 = 3529.9999999999995) tronque le montant d'un centime.
        val amountCents = abs((montant * 100).roundToLong())
        val type        = if (montant >= 0) TransactionType.INCOME else TransactionType.EXPENSE

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = devise,
            category     = BredCategoriseur.determinerCategorie(libelle, type),
            type         = type,
            note         = libelle,
            externalId   = BredCategoriseur.genererExternalId("bred", date, libelle, amountCents),
            rawType      = prefixeOp(libelle),
            importSource = "bred"
        )
    }

    /**
     * Parse une ligne du Format C (export "historique des opérations" bred.fr).
     * Colonnes fixes : Date(0) ; Référence(1) ; Type de l'opération(2) ; Catégorie(3) ;
     * Sous catégorie(4) ; Montant(5) ; Commentaire(6) ; Détail 1(7) ; Détail 2..6(8-12).
     *
     * Le libellé est reconstruit à partir du type d'opération + Détail 1 (ex. marchand
     * et date pour une carte) pour rester compatible avec les heuristiques de
     * [BredCategoriseur.determinerCategorie], écrites pour un libellé du style
     * "CARTE FNAC LE 12/04/26…" ou "PRELEVEMENT SEPA CANAL+ FRANCE".
     *
     * Colonnes optionnelles : `parseRow` ne restitue pas le dernier champ vide d'une
     * ligne qui se termine par ";" (voir ses tests), donc Détail 1 peut être absent
     * sur les lignes sans détail (ex. INTERETS FORFAITAIRES) — on tolère c.size >= 6.
     */
    private fun parseLigneFormatC(ligne: String): ImportedTransaction? {
        val c = parseRow(ligne)
        if (c.size < 6) return null

        val date    = parseDate(c[0]) ?: return null
        val typeOp  = c[2].trim()
        val montant = parseMontantFr(c[5]) ?: return null
        if (abs(montant) < 0.001) return null

        val detail1 = c.getOrNull(7)?.trim().orEmpty()
        val libelle = if (detail1.isNotBlank()) "$typeOp $detail1" else typeOp

        val amountCents = abs((montant * 100).roundToLong())
        val type        = if (montant >= 0) TransactionType.INCOME else TransactionType.EXPENSE

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = Currency.EUR,
            category     = BredCategoriseur.determinerCategorie(libelle, type),
            type         = type,
            note         = libelle,
            externalId   = BredCategoriseur.genererExternalId("bred", date, libelle, amountCents),
            rawType      = prefixeOp(libelle),
            importSource = "bred"
        )
    }

    /** Extrait les 2 premiers mots du libellé comme type d'opération BRED. */
    private fun prefixeOp(libelle: String): String =
        BredCategoriseur.normaliser(libelle).split(" ").take(2).joinToString(" ")

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private fun parseDate(s: String): LocalDate? =
        runCatching { LocalDate.parse(s.trim().removeSurrounding("\""), DATE_FORMAT) }.getOrNull()

    /**
     * Parse un montant au format français :
     * - Décimale avec virgule : "1 234,56" ou "45,99" ou "-120,50"
     * - Tiret long unicode (−) remplacé par tiret ASCII (-)
     * - Espaces insécables ( ) supprimés
     */
    private fun parseMontantFr(s: String): Double? {
        val net = s.trim().removeSurrounding("\"")
            .replace(" ", "")  // espace insécable (séparateur milliers fr)
            .replace(" ", "")       // espace ordinaire
            .replace(".", "")       // point comme séparateur milliers dans certains exports
            .replace(",", ".")      // virgule décimale → point
            .replace("−", "-") // tiret long unicode → tiret ASCII
        return net.toDoubleOrNull()
    }

    private fun parseCurrency(s: String): Currency = when (
        s.trim().removeSurrounding("\"").uppercase()
    ) {
        "USD" -> Currency.USD
        "XOF" -> Currency.XOF
        "XAF" -> Currency.XAF
        else  -> Currency.EUR
    }

    /**
     * Parseur CSV avec séparateur point-virgule, conforme RFC 4180
     * (gère les guillemets doublés à l'intérieur des valeurs).
     */
    private fun parseRow(ligne: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < ligne.length) {
            if (ligne[i] == '"') {
                val sb = StringBuilder()
                i++
                while (i < ligne.length) {
                    when {
                        ligne[i] == '"' && i + 1 < ligne.length && ligne[i + 1] == '"' -> {
                            sb.append('"'); i += 2
                        }
                        ligne[i] == '"' -> { i++; break }
                        else            -> { sb.append(ligne[i]); i++ }
                    }
                }
                result.add(sb.toString())
                if (i < ligne.length && ligne[i] == ';') i++
            } else {
                val fin = ligne.indexOf(';', i).takeIf { it >= 0 } ?: ligne.length
                result.add(ligne.substring(i, fin))
                i = if (fin < ligne.length) fin + 1 else fin
            }
        }
        return result
    }
}
