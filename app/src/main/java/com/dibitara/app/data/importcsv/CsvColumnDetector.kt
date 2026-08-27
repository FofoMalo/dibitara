package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.ModeMontant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Devine un [CsvColumnMapping] à partir d'un échantillon de lignes CSV déjà
 * découpées. Objet Kotlin pur.
 *
 * Deux sources d'indices, dans cet ordre :
 *  1. les noms d'en-tête, comparés à des dictionnaires de synonymes FR/EN
 *     (normalisés via [TexteNormalisation.cle]) ;
 *  2. le contenu des premières lignes de données (une colonne dont 60 % des
 *     valeurs parsent en date est la colonne date, etc.) — utilisé en repli
 *     quand il n'y a pas d'en-tête ou qu'un nom n'est pas reconnu.
 *
 * Si le résultat n'est pas [CsvColumnMapping.estComplet], l'appelant doit
 * présenter l'écran de mapping manuel, pré-rempli avec ce qui a été deviné.
 */
object CsvColumnDetector {

    /** Formats de date testés, dans l'ordre de préférence (FR avant US). */
    val FORMATS_DATE: List<String> = listOf(
        "dd/MM/yyyy", "yyyy-MM-dd", "dd.MM.yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy/MM/dd"
    )

    private val SYN_DATE = setOf(
        "date", "date operation", "date de l operation", "date d operation", "date valeur",
        "date comptable", "date de valeur", "booking date", "value date", "transaction date"
    )
    private val SYN_MONTANT = setOf(
        "montant", "amount", "valeur", "montant operation", "montant eur", "montant de l operation"
    )
    private val SYN_DEBIT = setOf("debit", "retrait", "sortie", "depense", "debits", "withdrawal")
    private val SYN_CREDIT = setOf("credit", "versement", "entree", "recette", "credits", "deposit")
    private val SYN_LIBELLE = setOf(
        "libelle", "libelle operation", "description", "nom de l operation", "detail", "details",
        "motif", "nature", "operation", "intitule", "communication", "reference", "memo", "payee",
        "transaction", "objet"
    )
    private val SYN_DEVISE = setOf("devise", "currency", "monnaie", "devise operation")

    /** Seuil de valeurs valides pour retenir une colonne par son contenu. */
    private const val SEUIL_CONTENU = 0.6

    fun detecter(rows: List<List<String>>, delimiteur: Char): CsvColumnMapping {
        val lignes = rows.filter { ligne -> ligne.any { it.isNotBlank() } }
        if (lignes.isEmpty()) return mappingVide(delimiteur)

        val nbColonnes = lignes.maxOf { it.size }
        val aEnTete = detecterEnTete(lignes)
        val enTete: List<String> =
            if (aEnTete) lignes.first().map { TexteNormalisation.cle(it) } else emptyList()
        val donnees = (if (aEnTete) lignes.drop(1) else lignes).take(20)

        fun colonneParEnTete(syns: Set<String>): Int? =
            enTete.indexOfFirst { entete -> entete.isNotBlank() && syns.any { entete == it || entete.contains(it) } }
                .takeIf { it >= 0 }

        // ─── Date ────────────────────────────────────────────────────────────
        val colDate = colonneParEnTete(SYN_DATE)
            ?: (0 until nbColonnes).maxByOrNull { c -> scoreDate(donnees, c) }
                ?.takeIf { scoreDate(donnees, it) >= SEUIL_CONTENU }
            ?: -1

        val formatDate = if (colDate >= 0) meilleurFormatDate(donnees, colDate) else FORMATS_DATE.first()

        // ─── Montant : signé vs débit/crédit ─────────────────────────────────
        val colDebit = colonneParEnTete(SYN_DEBIT)
        val colCredit = colonneParEnTete(SYN_CREDIT)
        val modeDebitCredit = colDebit != null && colCredit != null && colDebit != colCredit

        val colMontant = if (modeDebitCredit) null else {
            colonneParEnTete(SYN_MONTANT)
                ?: (0 until nbColonnes).filter { it != colDate }
                    .maxByOrNull { c -> scoreMontant(donnees, c) }
                    ?.takeIf { scoreMontant(donnees, it) >= SEUIL_CONTENU }
        }

        // ─── Séparateur décimal ──────────────────────────────────────────────
        val colonnesNumeriques = listOfNotNull(colMontant, colDebit, colCredit)
        val separateurDecimal = detecterSeparateurDecimal(donnees, colonnesNumeriques)

        // ─── Libellé ─────────────────────────────────────────────────────────
        val exclues = (listOf(colDate) + colonnesNumeriques + listOfNotNull(colonneParEnTete(SYN_DEVISE)))
            .filter { it >= 0 }.toSet()
        val colonnesLibelle = when {
            enTete.isNotEmpty() -> enTete.indices.filter { i ->
                val e = enTete[i]
                e.isNotBlank() && SYN_LIBELLE.any { e == it || e.contains(it) } && i !in exclues
            }
            else -> emptyList()
        }.ifEmpty { listOfNotNull(colonneTexteLaPlusRiche(donnees, nbColonnes, exclues)) }

        // ─── Devise ──────────────────────────────────────────────────────────
        val colDevise = colonneParEnTete(SYN_DEVISE)

        return CsvColumnMapping(
            delimiteur = delimiteur,
            aEnTete = aEnTete,
            colonneDate = colDate,
            formatDate = formatDate,
            modeMontant = if (modeDebitCredit) ModeMontant.DEBIT_CREDIT else ModeMontant.COLONNE_SIGNEE,
            colonneMontant = colMontant,
            colonneDebit = if (modeDebitCredit) colDebit else null,
            colonneCredit = if (modeDebitCredit) colCredit else null,
            separateurDecimal = separateurDecimal,
            colonnesLibelle = colonnesLibelle,
            colonneDevise = colDevise,
        )
    }

    // ─── Heuristiques ────────────────────────────────────────────────────────

    /**
     * En-tête présent si la 1re ligne ne contient aucune date parseable alors
     * qu'au moins une ligne suivante en contient une.
     */
    private fun detecterEnTete(lignes: List<List<String>>): Boolean {
        if (lignes.size < 2) return false
        val premiereADesDates = lignes.first().any { estUneDate(it) }
        val suiteADesDates = lignes.drop(1).take(5).any { ligne -> ligne.any { estUneDate(it) } }
        return !premiereADesDates && suiteADesDates
    }

    private fun scoreDate(donnees: List<List<String>>, col: Int): Double =
        proportion(donnees, col) { estUneDate(it) }

    private fun scoreMontant(donnees: List<List<String>>, col: Int): Double =
        proportion(donnees, col) { valeur ->
            val sep = MontantParser.detecterSeparateurDecimal(valeur)
            MontantParser.parse(valeur, sep) != null
        }

    private inline fun proportion(
        donnees: List<List<String>>,
        col: Int,
        predicat: (String) -> Boolean
    ): Double {
        val valeurs = donnees.mapNotNull { it.getOrNull(col)?.trim() }.filter { it.isNotBlank() }
        if (valeurs.isEmpty()) return 0.0
        return valeurs.count(predicat).toDouble() / valeurs.size
    }

    private fun estUneDate(valeur: String): Boolean {
        val v = valeur.trim().removeSurrounding("\"")
        if (v.length < 6) return false
        return FORMATS_DATE.any { fmt -> parseDate(v, fmt) != null }
    }

    private fun meilleurFormatDate(donnees: List<List<String>>, col: Int): String {
        val valeurs = donnees.mapNotNull { it.getOrNull(col)?.trim()?.removeSurrounding("\"") }
            .filter { it.isNotBlank() }
        return FORMATS_DATE.maxByOrNull { fmt -> valeurs.count { parseDate(it, fmt) != null } }
            ?: FORMATS_DATE.first()
    }

    private fun parseDate(valeur: String, format: String): LocalDate? = runCatching {
        LocalDate.parse(valeur, DateTimeFormatter.ofPattern(format))
    }.getOrNull()

    private fun detecterSeparateurDecimal(donnees: List<List<String>>, colonnes: List<Int>): Char {
        val valeurs = colonnes.flatMap { col ->
            donnees.mapNotNull { it.getOrNull(col)?.trim() }.filter { it.isNotBlank() }
        }
        if (valeurs.isEmpty()) return ','
        val virgules = valeurs.count { MontantParser.detecterSeparateurDecimal(it) == ',' }
        return if (virgules >= valeurs.size - virgules) ',' else '.'
    }

    /**
     * Colonne texte la plus « parlante » : celle dont les valeurs sont les plus
     * longues en moyenne, parmi les colonnes non déjà affectées et non numériques.
     */
    private fun colonneTexteLaPlusRiche(
        donnees: List<List<String>>,
        nbColonnes: Int,
        exclues: Set<Int>
    ): Int? = (0 until nbColonnes)
        .filter { it !in exclues }
        .filter { c -> scoreMontant(donnees, c) < SEUIL_CONTENU && scoreDate(donnees, c) < SEUIL_CONTENU }
        .maxByOrNull { c ->
            donnees.mapNotNull { it.getOrNull(c)?.trim()?.length }.average().let { if (it.isNaN()) 0.0 else it }
        }

    private fun mappingVide(delimiteur: Char) = CsvColumnMapping(
        delimiteur = delimiteur,
        aEnTete = false,
        colonneDate = -1,
        formatDate = FORMATS_DATE.first(),
        modeMontant = ModeMontant.COLONNE_SIGNEE,
        colonneMontant = null,
        separateurDecimal = ',',
        colonnesLibelle = emptyList(),
    )
}
