package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import java.io.InputStream
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

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
 * Deux variantes supportées :
 *   Format A — 4 colonnes : Date ; Libellé ; Montant ; Devise
 *   Format B — 5 colonnes : Date opération ; Date valeur ; Libellé ; Montant ; Devise
 *
 * Types d'opérations BRED reconnus (préfixes du libellé) :
 *   "Carte …"                      → paiement CB — catégorie déduite du nom du marchand
 *   "Prélèvement SEPA …"           → prélèvement récurrent — catégorie déduite du créancier
 *   "Prélèvement échéance …"       → remboursement crédit → LOGEMENT
 *   "Virement instantané émis …"   → virement sortant → TRANSFERTS
 *   "Virement instantané reçu …"   → virement entrant → AUTRE (revenu)
 *   "Virement automatique …"       → virement programmé → TRANSFERTS
 *   "Retrait d'espèces à un DAB …" → espèces → AUTRE
 *   "Cotisation …"                 → frais bancaires → AUTRE
 *   "Frais transaction carte …"    → frais → AUTRE
 *   "Intérêts forfaitaires …"      → agios/frais → AUTRE
 *
 * L'identifiant externe est calculé depuis date + libellé + montant car BRED n'expose
 * pas d'UUID — suffisant pour la détection de doublons entre deux imports successifs.
 */
object BredCsvParser {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // ─── Mots-clés pour la catégorisation automatique ────────────────────────
    // Tous en majuscules sans accents — la normalisation est appliquée avant le matching.

    private val MOTS_ALIMENTATION = setOf(
        "LECLERC", "CARREFOUR", "MONOPRIX", "LIDL", "ALDI", "SUPER U", "INTERMARCHE",
        "CASINO", "FRANPRIX", "G20", "BIOCOOP", "PICARD", "BOUCHERIE", "BOULANGERIE",
        "PATISSERIE", "TRAITEUR", "EPICERIE", "FROMAGERIE", "LADUREE", "BRIOCHE",
        "BISTRO", "BAR LE", "CLEMELANDRE", "DELICES", "COMPTOIR"
    )
    private val MOTS_TRANSPORT = setOf(
        "SNCF", "RATP", "UBER", "BLABLACAR", "TOTAL ENERGIE", "SHELL", "ESSO",
        "AUTOROUTE", "PARKING", "TAXI", "OUIGO", "AIR FRANCE", "EASYJET", "RYANAIR",
        "TRANSDEV", "KEOLIS", "UMS-ULYS", "ULYS MOBILITE", "SNBRSSLSAIR"
    )
    private val MOTS_SANTE = setOf(
        "PHARMACIE", "PHARMACIEN", "DOCTEUR", "MEDECIN", "CLINIQUE", "HOPITAL",
        "DENTISTE", "KINESITHERAPEUTE", "OPTICIEN", "LABORATOIRE", "MUTUELLE",
        "CPAM", "SECURITE SOCIALE"
    )
    private val MOTS_LOISIRS = setOf(
        "FNAC", "CINEMA", "THEATRE", "MUSEE", "NETFLIX", "DISNEY+", "AMAZON PRIME",
        "DEEZER", "SPOTIFY", "SALLE DE SPORT", "GYM", "PISCINE", "RELAY DAILY",
        "LES LIBRAIR", "LIBRAIRIE", "HOTEL", "BUY PARIS DUTY"
    )
    private val MOTS_HABILLEMENT = setOf(
        "H&M", "ZARA", "PRIMARK", "KIABI", "JULES", "CELIO", "DECATHLON",
        "SPORT 2000", "NIKE", "ADIDAS", "GALERIES LAFAYETTE", "UNIQLO"
    )
    private val MOTS_ABONNEMENTS = setOf(
        // Telecom
        "SFR", "ORANGE", "FREE MOBILE", "BOUYGUES TELECOM", "LA POSTE MOBILE",
        // Streaming & digital
        "ADOBE", "MICROSOFT", "GOOGLE", "APPLE.COM", "APPLE ", "OVH", "AMAZON WEB",
        "CANAL PLUS", "CANAL+", "MEDIAPART", "LIBERATION", "OUEST FRANCE",
        // Energie & eau
        "EDF", "ENGIE", "VEOLIA", "SUEZ", "ENEDIS", "EAU DE PARIS", "SAUR",
        // Assurances & epargne
        "GENERALI", "KEREIS", "APICIL", "MAIF", "AXA", "MACIF", "MGEN"
    )
    private val MOTS_LOGEMENT = setOf(
        "LOYER", "SYNDIC", "CHARGES COPROPRIETE", "LEROY MERLIN", "CASTORAMA",
        "IKEA", "BUT ", "CONFORAMA", "MAISONS DU MONDE", "BRICORAMA"
    )

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

        // Format B : la 2e colonne de l'en-tête contient "valeur" → décale les colonnes de 1
        val colonnes  = parseRow(lignes[indexEnTete])
        val offsetLib = if (colonnes.size >= 5 && colonnes[1].contains("valeur", ignoreCase = true)) 1 else 0

        return lignes.drop(indexEnTete + 1).mapNotNull { parseLigne(it, offsetLib) }
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
        val amountCents = abs((montant * 100).toLong())
        val type        = if (montant >= 0) TransactionType.INCOME else TransactionType.EXPENSE

        return ImportedTransaction(
            date         = date,
            amountCents  = amountCents,
            currency     = devise,
            category     = determinerCategorie(libelle, type),
            type         = type,
            note         = libelle,
            externalId   = genererExternalId(date, libelle, amountCents),
            rawType      = prefixeOp(libelle),
            importSource = "bred"
        )
    }

    // ─── Catégorisation par type d'opération BRED puis mots-clés ─────────────

    private fun determinerCategorie(libelle: String, type: TransactionType): Category {
        val n = normaliser(libelle) // majuscules + sans accents

        // 1. Frais et agios bancaires
        if (n.startsWith("COTISATION") || n.startsWith("FRAIS") || n.startsWith("INTERETS")) {
            return Category.AUTRE
        }

        // 2. Remboursement crédit (prêt habitat, personnel…)
        if (n.startsWith("PRELEVEMENT ECHEANCE") || n.startsWith("PRELEVEMENT ECH")) {
            return Category.LOGEMENT
        }

        // 3. Virements (entrant = revenu, sortant = transfert)
        // "Virement instantané émis/reçu" → VIREMENT… ; ancien format "VIR SEPA…" → VIR …
        if (n.startsWith("VIREMENT") || n.startsWith("VIR ")) {
            return if (type == TransactionType.INCOME) Category.AUTRE else Category.TRANSFERTS
        }

        // 4. Retraits
        if (n.startsWith("RETRAIT") || n.contains("DAB")) {
            return Category.AUTRE
        }

        // 5. Revenus non classés plus haut → AUTRE
        if (type == TransactionType.INCOME) return Category.AUTRE

        // 6. Prélèvements SEPA : déduire la catégorie depuis le nom du créancier
        if (n.startsWith("PRELEVEMENT SEPA") || n.startsWith("PRLV")) {
            val creancier = n.removePrefix("PRELEVEMENT SEPA").removePrefix("PRLV").trim()
            return verifierMotsCles(creancier) ?: Category.ABONNEMENTS
        }

        // 7. Paiements carte : extraire le nom du marchand (avant " le DD/MM/")
        if (n.startsWith("CARTE ")) {
            val marchand = extraireMarchand(n)
            return verifierMotsCles(marchand) ?: Category.AUTRE
        }

        // 8. Fallback : matching sur tout le libellé normalisé
        return verifierMotsCles(n) ?: Category.AUTRE
    }

    private fun verifierMotsCles(n: String): Category? = when {
        MOTS_ALIMENTATION.any { n.contains(it) } -> Category.ALIMENTATION
        MOTS_TRANSPORT.any    { n.contains(it) } -> Category.TRANSPORT
        MOTS_SANTE.any        { n.contains(it) } -> Category.SANTE
        MOTS_LOISIRS.any      { n.contains(it) } -> Category.LOISIRS
        MOTS_HABILLEMENT.any  { n.contains(it) } -> Category.HABILLEMENT
        MOTS_ABONNEMENTS.any  { n.contains(it) } -> Category.ABONNEMENTS
        MOTS_LOGEMENT.any     { n.contains(it) } -> Category.LOGEMENT
        else                                      -> null
    }

    /**
     * Extrait le nom du marchand depuis un libellé de paiement carte BRED.
     * Exemple : "Carte fnac le 12/04/26 cb.xxxxx9968 / origine : france / montant : 35,00 eur"
     *           → "FNAC"
     */
    private fun extraireMarchand(libelleNormalise: String): String {
        val sansPrefix = libelleNormalise.removePrefix("CARTE ").trim()
        // Tronquer au premier " LE DD/MM/" pour isoler le marchand
        val apresLe = sansPrefix.split(Regex(""" LE \d{2}/""")).first()
        return apresLe.split(Regex(""" CB\.""")).first().trim().ifBlank { sansPrefix.take(40).trim() }
    }

    /** Extrait les 2 premiers mots du libellé comme type d'opération BRED. */
    private fun prefixeOp(libelle: String): String =
        normaliser(libelle).split(" ").take(2).joinToString(" ")

    /**
     * Normalise une chaîne : majuscules + suppression des accents (é→E, è→E, etc.).
     * Nécessaire car les libellés BRED contiennent des accents ("Prélèvement", "échéance"…)
     * et nos mots-clés sont sans accents pour la robustesse.
     */
    private fun normaliser(s: String): String =
        Normalizer.normalize(s.uppercase(), Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")

    // ─── Génération de l'identifiant externe ──────────────────────────────────

    /**
     * BRED n'expose pas d'UUID — identifiant stable construit depuis date + libellé + centimes.
     * Collision possible si deux transactions strictement identiques le même jour, ce qui est
     * rare en pratique et détecté en amont par le use case de déduplication.
     */
    private fun genererExternalId(date: LocalDate, libelle: String, amountCents: Long): String {
        val cle = "${date}_${libelle.take(30).trim()}_$amountCents"
        return "bred_${Integer.toUnsignedString(cle.hashCode())}"
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private fun parseDate(s: String): LocalDate? =
        runCatching { LocalDate.parse(s.trim().removeSurrounding("\""), DATE_FORMAT) }.getOrNull()

    /**
     * Parse un montant au format français :
     * - Décimale avec virgule : "1 234,56" ou "45,99" ou "-120,50"
     * - Tiret long unicode (−) remplacé par tiret ASCII (-)
     * - Espaces insécables ( ) supprimés
     */
    private fun parseMontantFr(s: String): Double? {
        val net = s.trim().removeSurrounding("\"")
            .replace(" ", "")  // espace insécable (séparateur milliers fr)
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
