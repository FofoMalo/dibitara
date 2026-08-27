package com.dibitara.app.data.importcsv

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.TransactionType
import java.text.Normalizer
import java.time.LocalDate

/**
 * Logique partagée de catégorisation et de génération d'identifiant pour les imports BRED.
 * Utilisé par [BredCsvParser] (CSV) et [BredPdfParser] (PDF) pour éviter la duplication.
 */
internal object BredCategoriseur {

    // ─── Mots-clés par catégorie (majuscules sans accents) ───────────────────

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
        "SFR", "ORANGE", "FREE MOBILE", "BOUYGUES TELECOM", "LA POSTE MOBILE",
        "ADOBE", "MICROSOFT", "GOOGLE", "APPLE.COM", "APPLE ", "OVH", "AMAZON WEB",
        "CANAL PLUS", "CANAL+", "MEDIAPART", "LIBERATION", "OUEST FRANCE"
    )
    // Énergie et eau classées en LOGEMENT : nature budgétaire (maintien du foyer),
    // pas en ABONNEMENTS qui est réservé aux services numériques/téléphonie.
    private val MOTS_LOGEMENT = setOf(
        "LOYER", "SYNDIC", "CHARGES COPROPRIETE", "LEROY MERLIN", "CASTORAMA",
        "IKEA", "BUT ", "CONFORAMA", "MAISONS DU MONDE", "BRICORAMA",
        "EDF", "ENGIE", "VEOLIA", "SUEZ", "ENEDIS", "EAU DE PARIS", "SAUR"
    )
    // Assurances classées en IMPOTS_CHARGES : obligations légales et protection
    // patrimoniale, distinctes des abonnements de services numériques.
    private val MOTS_IMPOTS = setOf(
        "GENERALI", "KEREIS", "APICIL", "MAIF", "AXA", "MACIF", "MGEN",
        "IMPOTS", "DGFIP", "URSSAF", "CAF "
    )

    // ─── Catégorisation ───────────────────────────────────────────────────────

    /**
     * Détermine la catégorie d'une transaction BRED depuis son libellé brut.
     * Fonctionne avec le libellé CSV complet ("Carte fnac le 12/04…")
     * et avec la note PDF (typeOpération + description).
     */
    fun determinerCategorie(libelle: String, type: TransactionType): Category {
        val n = normaliser(libelle)

        if (n.startsWith("COTISATION") || n.startsWith("FRAIS") || n.startsWith("INTERETS")) {
            return Category.AUTRE
        }
        if (n.startsWith("PRELEVEMENT ECHEANCE") || n.startsWith("PRELEVEMENT ECH")) {
            return Category.LOGEMENT
        }
        if (n.startsWith("VIREMENT") || n.startsWith("VIR ")) {
            return if (type == TransactionType.INCOME) Category.AUTRE else Category.TRANSFERTS
        }
        if (n.startsWith("RETRAIT") || n.contains("DAB")) {
            return Category.AUTRE
        }
        if (type == TransactionType.INCOME) return Category.AUTRE

        if (n.startsWith("PRELEVEMENT SEPA") || n.startsWith("PRLV")) {
            val creancier = n.removePrefix("PRELEVEMENT SEPA").removePrefix("PRLV").trim()
            return verifierMotsCles(creancier) ?: Category.ABONNEMENTS
        }
        if (n.startsWith("CARTE ")) {
            val marchand = extraireMarchand(n)
            return verifierMotsCles(marchand) ?: Category.AUTRE
        }

        return verifierMotsCles(n) ?: Category.AUTRE
    }

    fun verifierMotsCles(n: String): Category? = when {
        MOTS_ALIMENTATION.any { n.contains(it) } -> Category.ALIMENTATION
        MOTS_TRANSPORT.any    { n.contains(it) } -> Category.TRANSPORT
        MOTS_SANTE.any        { n.contains(it) } -> Category.SANTE
        MOTS_LOISIRS.any      { n.contains(it) } -> Category.LOISIRS
        MOTS_HABILLEMENT.any  { n.contains(it) } -> Category.HABILLEMENT
        MOTS_ABONNEMENTS.any  { n.contains(it) } -> Category.ABONNEMENTS
        MOTS_LOGEMENT.any     { n.contains(it) } -> Category.LOGEMENT
        MOTS_IMPOTS.any       { n.contains(it) } -> Category.IMPOTS_CHARGES
        else                                      -> null
    }

    /**
     * Extrait le nom du marchand depuis un libellé "Carte" normalisé.
     * Exemple : "CARTE FNAC LE 12/04/26 CB.XXXXX9968…" → "FNAC"
     */
    fun extraireMarchand(libelleNormalise: String): String {
        val sansPrefix = libelleNormalise.removePrefix("CARTE ").trim()
        val apresLe = sansPrefix.split(Regex(""" LE \d{2}/""")).first()
        return apresLe.split(Regex(""" CB\.""")).first().trim().ifBlank { sansPrefix.take(40).trim() }
    }

    // ─── Identifiant externe ─────────────────────────────────────────────────

    /**
     * Identifiant stable : prefix + hash(date + libellé + centimes).
     * Même fonction utilisée par CSV et PDF → un doublon CSV/PDF est détecté si même note courte.
     */
    fun genererExternalId(prefix: String, date: LocalDate, libelle: String, amountCents: Long): String {
        val cle = "${date}_${libelle.take(30).trim()}_$amountCents"
        return "${prefix}_${Integer.toUnsignedString(cle.hashCode())}"
    }

    /**
     * Identifiant sans libellé : utilisé pour les captures live (notifications push),
     * dont le texte ne permet pas de reconstituer un libellé comparable à celui du CSV.
     * Permet la réconciliation ultérieure par montant + date quand le CSV est importé.
     */
    fun genererExternalIdMontantDate(prefix: String, date: LocalDate, amountCents: Long): String {
        val cle = "${date}_$amountCents"
        return "${prefix}_${Integer.toUnsignedString(cle.hashCode())}"
    }

    // ─── Normalisation ────────────────────────────────────────────────────────

    /** Majuscules + suppression des accents via décomposition NFD. */
    fun normaliser(s: String): String =
        Normalizer.normalize(s.uppercase(), Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
}
