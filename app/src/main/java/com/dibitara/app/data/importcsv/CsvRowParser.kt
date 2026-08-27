package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.ModeMontant
import com.dibitara.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Convertit les lignes de données d'un CSV (déjà découpées) en
 * [ImportedTransaction], selon un [CsvColumnMapping]. Objet Kotlin pur.
 *
 * La catégorie n'est **pas** déterminée ici (toujours [Category.AUTRE]) :
 * `AnalyserCsvUseCase` applique ensuite la suggestion, qui a besoin d'un accès
 * base (règles apprises).
 */
object CsvRowParser {

    data class Resultat(
        val transactions: List<ImportedTransaction>,
        val lignesIgnorees: Int,
    )

    fun parser(
        donnees: List<List<String>>,
        mapping: CsvColumnMapping,
        deviseParDefaut: Currency,
    ): Resultat {
        val formatteur = DateTimeFormatter.ofPattern(mapping.formatDate)
        var ignorees = 0
        val transactions = donnees.mapNotNull { ligne ->
            parserLigne(ligne, mapping, deviseParDefaut, formatteur).also { if (it == null) ignorees++ }
        }
        return Resultat(transactions, ignorees)
    }

    private fun parserLigne(
        ligne: List<String>,
        mapping: CsvColumnMapping,
        deviseParDefaut: Currency,
        formatteur: DateTimeFormatter,
    ): ImportedTransaction? {
        val date = champ(ligne, mapping.colonneDate)
            ?.let { runCatching { LocalDate.parse(it, formatteur) }.getOrNull() }
            ?: return null

        val (montant, type) = montantEtType(ligne, mapping) ?: return null
        val amountCents = abs((montant * 100).roundToLong())
        if (amountCents == 0L) return null

        val note = mapping.colonnesLibelle
            .mapNotNull { champ(ligne, it) }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val devise = mapping.colonneDevise
            ?.let { champ(ligne, it) }
            ?.let { deviseDepuis(it) }
            ?: deviseParDefaut

        return ImportedTransaction(
            date = date,
            amountCents = amountCents,
            currency = devise,
            type = type,
            note = note,
            category = Category.AUTRE,
            externalId = ExternalIdGenerator.pour(date, amountCents, note),
        )
    }

    /**
     * Retourne (montant absolu en unités, type). Null si aucun montant exploitable.
     *  - COLONNE_SIGNEE : signe du montant → dépense / revenu
     *  - DEBIT_CREDIT   : colonne débit renseignée → dépense ; colonne crédit → revenu
     */
    private fun montantEtType(
        ligne: List<String>,
        mapping: CsvColumnMapping,
    ): Pair<Double, TransactionType>? = when (mapping.modeMontant) {
        ModeMontant.COLONNE_SIGNEE -> {
            val brut = champ(ligne, mapping.colonneMontant ?: return null) ?: return null
            val valeur = MontantParser.parse(brut, mapping.separateurDecimal) ?: return null
            val type = if (valeur >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            abs(valeur) to type
        }
        ModeMontant.DEBIT_CREDIT -> {
            val debit = champ(ligne, mapping.colonneDebit ?: return null)
                ?.let { MontantParser.parse(it, mapping.separateurDecimal) }
                ?.takeIf { abs(it) > 0.0001 }
            val credit = champ(ligne, mapping.colonneCredit ?: return null)
                ?.let { MontantParser.parse(it, mapping.separateurDecimal) }
                ?.takeIf { abs(it) > 0.0001 }
            when {
                credit != null -> abs(credit) to TransactionType.INCOME
                debit != null -> abs(debit) to TransactionType.EXPENSE
                else -> null
            }
        }
    }

    /** Champ à l'index [i], trimé, ou null si absent / vide / index négatif. */
    private fun champ(ligne: List<String>, i: Int): String? =
        ligne.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }

    /** "EUR" / "€" / "usd"… → [Currency], ou null si non reconnu. "FCFA" seul → XOF. */
    private fun deviseDepuis(brut: String): Currency? {
        val v = brut.trim().uppercase().removeSurrounding("\"")
        Currency.entries.firstOrNull { it.isoCode == v }?.let { return it }
        return when (v) {
            "€", "EURO", "EUROS" -> Currency.EUR
            "$", "US$", "USD $" -> Currency.USD
            "FCFA", "CFA", "F CFA" -> Currency.XOF
            else -> null
        }
    }
}
