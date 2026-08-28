package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.repository.CategorizationRuleRepository

/**
 * Cascade de suggestion de catégorie pour une transaction, à partir de son libellé.
 *
 * Partagée entre l'import CSV ([SuggererCategorieImportUseCase]) et la
 * recatégorisation du Dashboard ([GetRecategorizationSuggestionsUseCase]). Ces
 * deux chemins avaient chacun leur copie de la logique de priorité, ce qui a
 * déjà provoqué une divergence : une règle apprise « AUTRE + sous-catégorie
 * personnalisée » était respectée par le Dashboard mais ignorée à l'import.
 *
 * Priorité, dans l'ordre :
 *  1. règle apprise par l'utilisateur ([CategorizationRuleRepository]) — sauf si
 *     elle ne porte AUCUNE catégorisation réelle (`AUTRE`, sans [SubCategory] ni
 *     sous-catégorie personnalisée) : on l'ignore alors et on tente les mots-clés ;
 *  2. dictionnaire de catégorie principale ([CategoriseurLibelle]) ;
 *  3. dictionnaire de sous-catégorie dans `AUTRE` ([REGLES_SOUS_CATEGORIES]).
 *
 * Objet Kotlin pur : le [CategorizationRuleRepository] est passé en paramètre
 * (comme [CategoriseurLibelle], on n'injecte pas dans un `object`).
 */
internal object CascadeCategorisation {

    /**
     * Résultat de la cascade.
     * [motCle] ne sert qu'à l'affichage (« reconnu grâce à … » sur le Dashboard) ;
     * l'import ne s'en sert pas.
     */
    data class Suggestion(
        val category: Category,
        val subCategory: SubCategory? = null,
        val customSubCategoryId: Long? = null,
        val motCle: String? = null,
    )

    /** Retourne null si rien ne correspond (ni règle exploitable, ni mot-clé). */
    suspend fun suggerer(note: String, ruleRepository: CategorizationRuleRepository): Suggestion? {
        val libelle = note.lowercase().trim()
        if (libelle.isBlank()) return null

        // 1. Règle apprise. Une règle « AUTRE » sans SubCategory ni sous-catégorie
        //    perso n'apporte rien — on l'ignore pour tenter les mots-clés. Oublier
        //    `customSubCategoryId` ici est le piège documenté dans CLAUDE.md
        //    (« Category.AUTRE seul ≠ non catégorisé »).
        val regle = ruleRepository.getRuleForNote(note)
        if (regle != null) {
            val regleSansCategorisation = regle.category == Category.AUTRE &&
                regle.subCategory == null &&
                regle.customSubCategoryId == null
            if (!regleSansCategorisation) {
                return Suggestion(
                    category = regle.category,
                    subCategory = regle.subCategory,
                    customSubCategoryId = regle.customSubCategoryId,
                    motCle = note.trim(),
                )
            }
        }

        // 2. Catégorie principale par mot-clé.
        CategoriseurLibelle.suggererCategorie(note)?.let { categorie ->
            return Suggestion(category = categorie, motCle = trouverMotCle(libelle, categorie))
        }

        // 3. Sous-catégorie de mot-clé dans AUTRE.
        for (regleSc in REGLES_SOUS_CATEGORIES) {
            val mot = regleSc.motsCles.firstOrNull { libelle.correspondMotCle(it) }
            if (mot != null) {
                return Suggestion(Category.AUTRE, subCategory = regleSc.sousCategorie, motCle = mot)
            }
        }

        return null
    }

    /**
     * Retrouve le premier mot-clé qui correspond dans [libelle] (déjà en minuscules)
     * parmi les règles de [CategoriseurLibelle], filtré sur [categorie].
     * Sert uniquement à renseigner [Suggestion.motCle].
     */
    private fun trouverMotCle(libelle: String, categorie: Category): String? =
        MOTS_CLES_PAR_CATEGORIE[categorie]?.firstOrNull { libelle.correspondMotCle(it) }

    /**
     * Correspond un mot-clé dans le libellé (déjà en minuscules) :
     * - Expression multi-mots (contient un espace) → recherche substring classique.
     * - Mot seul → correspondance token exacte après découpage sur espaces et
     *   ponctuation courante. Le trait d'union est exclu du découpage pour
     *   préserver les mots composés comme "station-service".
     */
    private fun String.correspondMotCle(motCle: String): Boolean =
        if (motCle.contains(' ')) this.contains(motCle)
        else this.split(Regex("[\\s./,;:!?()|@]+")).any { it == motCle }

    private data class RegleSousCategorie(val motsCles: List<String>, val sousCategorie: SubCategory)

    /**
     * Table inversée catégorie → mots-clés, dérivée de [CategoriseurLibelle].
     * Utilisée uniquement pour retrouver le mot-clé correspondant ([trouverMotCle]).
     */
    private val MOTS_CLES_PAR_CATEGORIE: Map<Category, List<String>> = mapOf(
        Category.TRANSFERTS to listOf("orange money", "wave", "mtn momo", "mtn mobile",
            "moov money", "free money", "airtel money", "m-pesa",
            "virement", "sepa", "remise", "remboursement"),
        Category.ALIMENTATION to listOf("leclerc", "carrefour", "lidl", "aldi",
            "intermarché", "casino", "monoprix", "franprix", "picard", "biocoop",
            "marché", "epicerie", "supermarché", "boulangerie", "shoprite",
            "citydia", "auchan"),
        Category.LOGEMENT to listOf("loyer", "charges", "syndic", "copropriété",
            "électricité", "edf", "engie", "gaz", "eau", "veolia", "suez",
            "internet", "fibre"),
        Category.TRANSPORT to listOf("sncf", "ratp", "transilien", "uber", "essence",
            "péage", "autoroute", "parking", "station-service", "total", "bp",
            "shell", "vinci autoroutes"),
        Category.SANTE to listOf("pharmacie", "médecin", "docteur", "hôpital",
            "clinique", "mutuelle", "ameli", "ophtalmo", "dentiste", "kiné",
            "infirmier"),
        Category.ABONNEMENTS to listOf("netflix", "spotify", "deezer", "amazon prime",
            "disney+", "canal+", "sfr", "bouygues", "orange", "free mobile",
            "numéricable", "adobe", "microsoft 365", "icloud", "google one"),
        Category.LOISIRS to listOf("gym", "fitness", "cinema", "cinéma", "théâtre",
            "musée", "piscine", "sport", "loisirs", "fnac", "culture", "concert",
            "festival", "hotel"),
        Category.IMPOTS_CHARGES to listOf("impôts", "dgfip", "caf", "cpam", "urssaf",
            "assurance habitation", "assurance auto", "allianz", "axa", "maif",
            "macif", "groupama"),
        Category.HABILLEMENT to listOf("zara", "h&m", "uniqlo", "nike", "adidas",
            "décathlon", "vêtement", "chaussures", "habillement")
    )

    /**
     * Dictionnaire de règles de sous-catégorisation dans `AUTRE`.
     * Utilisé quand aucune catégorie principale ne correspond.
     * `DIVERS` est exclu : c'est la valeur de refus, pas une suggestion automatique.
     */
    private val REGLES_SOUS_CATEGORIES = listOf(
        RegleSousCategorie(
            listOf("cadeau", "cadeaux", "anniversaire", "noël", "noel", "fête"),
            SubCategory.CADEAUX
        ),
        RegleSousCategorie(
            listOf("frais bancaires", "commission bancaire", "cotisation carte",
                "agios", "découvert", "frais de tenue", "frais de dossier",
                "ecobank", "uba", "coris bank"),
            SubCategory.FRAIS_BANCAIRES
        ),
        RegleSousCategorie(
            listOf("restaurant", "bar", "café", "cafe", "bistro", "brasserie",
                "pizzeria", "kebab", "burger", "sushi", "snack", "terrasse"),
            SubCategory.BAR_ET_RESTAURANT
        )
    )
}
