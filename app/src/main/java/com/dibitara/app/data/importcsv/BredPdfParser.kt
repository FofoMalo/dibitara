package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.time.LocalDate

/**
 * Parse le texte brut extrait d'un relevé PDF BRED et retourne les transactions et soldes.
 *
 * Structure d'un relevé BRED (texte pdfbox, sortByPosition=true) :
 *   - En-tête : "4 mai 2026, compte 518.06.4209, relevé n°5" → extraction de l'année
 *   - Section "Situation de vos comptes" → [SoldeCompte] : label, plafond optionnel, solde
 *   - Section "Relevé d'opérations du poste principal" → transactions ligne par ligne
 *
 * Format d'une ligne de transaction :
 *   DD.MM  TYPE [REF]  MONTANT  [DD.MM.YY]
 *   [description éventuellement sur plusieurs lignes suivantes]
 *
 * La catégorisation est déléguée à [BredCategoriseur] (partagé avec [BredCsvParser]).
 *
 * Cette classe est pure Kotlin (aucune dépendance Android) et donc entièrement testable
 * via des JUnit 5 unitaires en passant du texte synthétique à [parseTexte].
 */
object BredPdfParser {

    data class SoldeCompte(
        val label: String,
        val plafondCents: Long?,
        val soldeCents: Long
    )

    data class ParseResult(
        val transactions: List<ImportedTransaction>,
        val soldes: List<SoldeCompte>,
        val annee: Int
    )

    // ─── Regexs ──────────────────────────────────────────────────────────────

    // Date de transaction : DD.MM en début de ligne, non suivi de .YY (date valeur)
    private val DATE_TX = Regex("""^(\d{2})\.(\d{2})(?!\.\d{2})\s+(.+)$""")

    // Montant français en fin de ligne, avant date valeur optionnelle.
    // Séparateur milliers : point UNIQUEMENT (pas espace - évite de confondre "94AD670 377,00").
    // Exemples : "48,20 07.04.26"  "69,99 07.04.26"  "377,00"  "2.879,25 07.04.26"
    private val MONTANT_FIN = Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})\s*(?:\d{2}\.\d{2}\.\d{2})?\s*$""")

    // Montant français n'importe où dans une ligne (pour la section soldes)
    private val MONTANT_DANS_LIGNE = Regex("""\d{1,3}(?:\.\d{3})*,\d{2}""")

    // Année dans l'en-tête : "4 mai 2026" ou "14 novembre 2025"
    private val ANNEE_HEADER = Regex("""\d+\s+\w+\s+(\d{4})""")

    // Référence à supprimer : token alphanumérique ≥ 6 chars en fin du type d'opération
    private val REFERENCE_FIN = Regex("""\s+[A-Za-z0-9]{6,}\s*$""")

    // ─── Point d'entrée ──────────────────────────────────────────────────────

    /**
     * Parse le texte brut d'un relevé PDF BRED.
     * [texte] est la sortie de [PDFTextStripper] avec [sortByPosition] = true.
     */
    fun parseTexte(texte: String): ParseResult {
        val lignes = texte.lines().map { nettoyerTexte(it) }
        val lignesNorm = lignes.map { BredCategoriseur.normaliser(it) }

        val annee = ANNEE_HEADER.find(texte)?.groupValues?.get(1)?.toIntOrNull()
            ?: LocalDate.now().year

        val soldes = extraireSoldes(lignes, lignesNorm)
        val transactions = extraireTransactions(lignes, lignesNorm, annee)

        return ParseResult(transactions, soldes, annee)
    }

    // ─── Section "Situation de vos comptes" ──────────────────────────────────

    private fun extraireSoldes(lignes: List<String>, lignesNorm: List<String>): List<SoldeCompte> {
        val idxDebut = lignesNorm.indexOfFirst { it.contains("SITUATION DE VOS COMPTES") }
        if (idxDebut < 0) return emptyList()

        // La section se termine à la première ligne "RELEVE D'OPERATIONS"
        val idxFin = lignesNorm.drop(idxDebut + 1)
            .indexOfFirst { it.contains("RELEVE") && it.contains("OPERATIONS") }
            .takeIf { it >= 0 }?.let { idxDebut + 1 + it } ?: lignes.size

        val soldes = mutableListOf<SoldeCompte>()

        for (i in (idxDebut + 1) until idxFin) {
            val ligne = lignes[i]
            val montants = MONTANT_DANS_LIGNE.findAll(ligne).map { it.value }.toList()
            if (montants.isEmpty()) continue

            // Label = tout ce qui précède le premier montant dans la ligne
            val idxPremier = ligne.indexOf(montants.first())
            val label = if (idxPremier > 0) ligne.substring(0, idxPremier).trim() else continue
            if (label.isBlank()) continue

            // Ignorer les lignes d'en-tête de tableau (ex: "Plafond", "Solde au DD/MM/YY")
            val labelNorm = BredCategoriseur.normaliser(label)
            if (labelNorm.startsWith("PLAFOND") || labelNorm.startsWith("SOLDE") ||
                labelNorm.startsWith("COMPTE")) continue

            // 2 montants → plafond + solde ; 1 montant → solde seulement (pas de plafond)
            val plafond = if (montants.size >= 2) parseMontantFr(montants[0]) else null
            val solde   = parseMontantFr(montants.last()) ?: continue

            soldes.add(SoldeCompte(
                label        = label,
                plafondCents = plafond?.let { (it * 100).toLong() },
                soldeCents   = (solde * 100).toLong()
            ))
        }

        return soldes
    }

    // ─── Section "Relevé d'opérations" ───────────────────────────────────────

    private fun extraireTransactions(
        lignes: List<String>,
        lignesNorm: List<String>,
        annee: Int
    ): List<ImportedTransaction> {
        val idxOps = lignesNorm.indexOfFirst {
            it.contains("RELEVE") && it.contains("OPERATIONS")
        }
        if (idxOps < 0) return emptyList()

        val transactions = mutableListOf<ImportedTransaction>()
        var txCourante: TxPartielle? = null
        val noteCourante = StringBuilder()

        for (i in (idxOps + 1) until lignes.size) {
            val ligne = lignes[i].trim()
            if (ligne.isBlank()) continue

            val match = DATE_TX.matchEntire(ligne)

            if (match != null) {
                // Finaliser la transaction en cours avant d'en commencer une nouvelle
                txCourante?.let { tx ->
                    construireTransaction(tx, noteCourante.toString().trim(), annee)
                        ?.let { transactions.add(it) }
                }

                val jour  = match.groupValues[1].toInt()
                val mois  = match.groupValues[2].toInt()
                val reste = match.groupValues[3]

                // Ignorer "Solde précédent" et "Solde au"
                if (BredCategoriseur.normaliser(reste).startsWith("SOLDE")) {
                    txCourante = null
                    noteCourante.clear()
                    continue
                }

                // Extraire le montant en fin de ligne
                val montantMatch = MONTANT_FIN.find(reste)
                if (montantMatch == null) {
                    txCourante = null
                    noteCourante.clear()
                    continue
                }

                val montantVal = parseMontantFr(montantMatch.groupValues[1])
                if (montantVal == null || montantVal <= 0.0) {
                    txCourante = null
                    noteCourante.clear()
                    continue
                }

                // Le type est ce qui précède le montant, sans le code de référence
                val typeAvecRef = reste.substring(0, montantMatch.range.first).trim()
                val typeNet     = supprimerReference(typeAvecRef)

                txCourante = TxPartielle(jour, mois, typeNet, typeAvecRef, montantVal)
                noteCourante.clear()
                noteCourante.append(typeAvecRef)

            } else if (txCourante != null) {
                // Ligne de description (suite de la transaction précédente)
                if (noteCourante.isNotEmpty()) noteCourante.append(" ")
                noteCourante.append(ligne)
            }
        }

        // Finaliser la dernière transaction
        txCourante?.let { tx ->
            construireTransaction(tx, noteCourante.toString().trim(), annee)
                ?.let { transactions.add(it) }
        }

        return transactions
    }

    private data class TxPartielle(
        val jour: Int,
        val mois: Int,
        val typeNet: String,     // type sans référence (ex: "Carte")
        val typeAvecRef: String, // type avec référence (ex: "Carte 4217162")
        val montantVal: Double
    )

    private fun construireTransaction(tx: TxPartielle, note: String, annee: Int): ImportedTransaction? {
        val date = try {
            LocalDate.of(annee, tx.mois, tx.jour)
        } catch (e: Exception) { return null }

        val amountCents = (tx.montantVal * 100).toLong()
        val typeNorm = BredCategoriseur.normaliser(tx.typeNet)

        // Revenu si le type contient "reçu" / "recu" (virement entrant)
        val transactionType = if (typeNorm.contains("RECU")) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        // La note contient le type avec référence + description → utilisée pour catégoriser
        val noteComplete = note.take(200).ifBlank { tx.typeNet }

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = Currency.EUR,
            category     = BredCategoriseur.determinerCategorie(noteComplete, transactionType),
            type         = transactionType,
            note         = noteComplete,
            externalId   = BredCategoriseur.genererExternalId("bred", date, note.take(30), amountCents),
            rawType      = tx.typeNet.take(50),
            importSource = "bred"
        )
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    /**
     * Supprime le code de référence en fin de type (token alphanumérique ≥ 6 chars).
     * Exemples : "Carte 4217162" → "Carte"
     *            "Prélèvement SEPA 2897174" → "Prélèvement SEPA"
     *            "Prélèvement echéance 005 de votre" → inchangé (005 < 6 chars)
     */
    private fun supprimerReference(type: String): String =
        REFERENCE_FIN.replace(type, "").trim()

    /**
     * Supprime les caractères hors Latin-1 (code > 255) - retire ①②③ (U+2460+) tout en
     * conservant les accents français (é, è, à, ç… U+00C0–U+00FF).
     * Normalise aussi les espaces multiples.
     */
    private fun nettoyerTexte(s: String): String =
        s.filter { it.code <= 0xFF }.replace(Regex("""\s+"""), " ").trim()

    private fun parseMontantFr(s: String): Double? {
        val net = s.trim()
            .replace(" ", "") // espace insécable
            .replace(" ", "")
            .replace(".", "")      // séparateur milliers
            .replace(",", ".")     // décimale fr → point
        return net.toDoubleOrNull()
    }
}
