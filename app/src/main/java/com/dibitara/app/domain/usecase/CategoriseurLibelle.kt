package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category

/**
 * Suggère une catégorie principale à partir du libellé d'une transaction.
 *
 * Utilisé à l'import (recatégorisation automatique) et par
 * [GetRecategorizationSuggestionsUseCase] pour les transactions existantes.
 *
 * Les règles Mobile Money sont en tête pour éviter que "orange" (mot seul)
 * ne soit capturé par la règle ABONNEMENTS avant "orange money" (expression).
 */
internal object CategoriseurLibelle {

    /**
     * Retourne la catégorie principale suggérée pour [note], ou null si aucune règle ne correspond.
     * [note] peut être dans n'importe quelle casse - la comparaison est insensible à la casse.
     */
    fun suggererCategorie(note: String): Category? {
        val libelle = note.lowercase().trim()
        if (libelle.isBlank()) return null
        return REGLES_MOTS_CLES.firstNotNullOfOrNull { regle ->
            if (regle.motsCles.any { libelle.correspondMotCle(it) }) regle.categorie else null
        }
    }

    /**
     * Correspond un mot-clé dans le libellé (déjà en minuscules) :
     * - Expression multi-mots (contient un espace) → recherche substring classique.
     * - Mot seul → correspondance token exacte après découpage sur espaces et ponctuation
     *   courante (point, slash, virgule…). Évite les faux positifs : "eau" ne matche plus
     *   "cadeau", "gaz" ne matche plus "magasin". Le trait d'union est exclu du découpage
     *   pour préserver les mots composés comme "station-service".
     */
    private fun String.correspondMotCle(motCle: String): Boolean =
        if (motCle.contains(' ')) this.contains(motCle)
        else this.split(Regex("[\\s./,;:!?()|@]+")).any { it == motCle }

    private data class RegleCategorisation(val motsCles: List<String>, val categorie: Category)

    private val REGLES_MOTS_CLES = listOf(
        // Mobile Money Afrique - doit précéder ABONNEMENTS ("orange" y est en mot seul)
        RegleCategorisation(
            listOf("orange money", "wave", "mtn momo", "mtn mobile", "moov money",
                "free money", "airtel money", "m-pesa"),
            Category.TRANSFERTS
        ),
        RegleCategorisation(
            listOf("leclerc", "carrefour", "lidl", "aldi", "intermarché", "casino", "monoprix",
                "franprix", "picard", "biocoop", "marché", "epicerie", "supermarché",
                "boulangerie", "shoprite", "citydia", "auchan"),
            Category.ALIMENTATION
        ),
        RegleCategorisation(
            listOf("loyer", "charges", "syndic", "copropriété", "électricité", "edf", "engie",
                "gaz", "eau", "veolia", "suez", "internet", "fibre"),
            Category.LOGEMENT
        ),
        RegleCategorisation(
            listOf("sncf", "ratp", "transilien", "uber", "essence", "péage", "autoroute",
                "parking", "station-service", "total", "bp", "shell", "vinci autoroutes"),
            Category.TRANSPORT
        ),
        RegleCategorisation(
            listOf("pharmacie", "médecin", "docteur", "hôpital", "clinique", "mutuelle",
                "ameli", "ophtalmo", "dentiste", "kiné", "infirmier"),
            Category.SANTE
        ),
        RegleCategorisation(
            listOf("netflix", "spotify", "deezer", "amazon prime", "disney+", "canal+",
                "sfr", "bouygues", "orange", "free mobile", "numéricable", "adobe",
                "microsoft 365", "icloud", "google one"),
            Category.ABONNEMENTS
        ),
        RegleCategorisation(
            listOf("gym", "fitness", "cinema", "cinéma", "théâtre", "musée", "piscine",
                "sport", "loisirs", "fnac", "culture", "concert", "festival", "hotel"),
            Category.LOISIRS
        ),
        RegleCategorisation(
            listOf("impôts", "dgfip", "caf", "cpam", "urssaf", "assurance habitation",
                "assurance auto", "allianz", "axa", "maif", "macif", "groupama"),
            Category.IMPOTS_CHARGES
        ),
        RegleCategorisation(
            listOf("virement", "sepa", "remise", "remboursement"),
            Category.TRANSFERTS
        ),
        RegleCategorisation(
            listOf("zara", "h&m", "uniqlo", "nike", "adidas", "décathlon", "vêtement",
                "chaussures", "habillement"),
            Category.HABILLEMENT
        )
    )
}
