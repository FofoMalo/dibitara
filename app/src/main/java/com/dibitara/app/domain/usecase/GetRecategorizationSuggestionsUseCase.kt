package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Analyse les transactions récentes catégorisées dans [Category.AUTRE] et propose
 * de meilleures catégories en comparant le libellé ([Transaction.note]) à un dictionnaire
 * de mots-clés par catégorie.
 *
 * Seules les transactions des 90 derniers jours sont analysées pour éviter de remonter
 * des entrées trop anciennes qui n'ont plus de pertinence.
 *
 * Une transaction avec [Transaction.subCategory] déjà renseignée est exclue : cela signifie
 * que l'utilisateur a déjà statué (refus via SubCategory.DIVERS ou autre choix manuel).
 *
 * [today] est injectable pour les tests.
 */
class GetRecategorizationSuggestionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<List<RecategorizationSuggestion>> {
        val debut = today.minusDays(90)
        return transactionRepository.getByDateRange(debut, today).map { transactions ->
            transactions
                .filter { it.category == Category.AUTRE && it.subCategory == null }
                .mapNotNull { trouverSuggestion(it) }
                .distinctBy { it.transaction.id }
        }
    }

    private fun trouverSuggestion(transaction: Transaction): RecategorizationSuggestion? {
        val libelle = transaction.note.lowercase().trim()
        if (libelle.isBlank()) return null

        // Priorité aux règles de catégorie principale
        for (regle in REGLES_MOTS_CLES) {
            val motCorrespondant = regle.motsCles.firstOrNull { libelle.correspondMotCle(it) }
            if (motCorrespondant != null) {
                return RecategorizationSuggestion(
                    transaction       = transaction,
                    suggestedCategory = regle.categorie,
                    matchedKeyword    = motCorrespondant
                )
            }
        }

        // Si aucune catégorie principale ne correspond, chercher une sous-catégorie d'AUTRE
        for (regle in REGLES_SOUS_CATEGORIES) {
            val motCorrespondant = regle.motsCles.firstOrNull { libelle.correspondMotCle(it) }
            if (motCorrespondant != null) {
                return RecategorizationSuggestion(
                    transaction          = transaction,
                    suggestedCategory    = Category.AUTRE,
                    matchedKeyword       = motCorrespondant,
                    suggestedSubCategory = regle.sousCategorie
                )
            }
        }

        return null
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

    // Règle associant une liste de mots-clés à la catégorie principale à proposer
    private data class RegleCategorisation(val motsCles: List<String>, val categorie: Category)

    // Règle associant une liste de mots-clés à une sous-catégorie d'AUTRE
    private data class RegleSousCategorie(val motsCles: List<String>, val sousCategorie: SubCategory)

    companion object {
        /**
         * Dictionnaire de règles de recatégorisation.
         * Correspondance via [correspondMotCle] : mot seul = token exact, expression = substring.
         * L'ordre des règles importe : la première correspondance l'emporte.
         * Les règles Mobile Money sont en tête pour que "orange money" soit testé
         * avant le mot seul "orange" présent dans la règle ABONNEMENTS.
         */
        private val REGLES_MOTS_CLES = listOf(
            // Mobile Money Afrique — doit précéder ABONNEMENTS ("orange" y est en mot seul)
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

        /**
         * Dictionnaire de règles de sous-catégorisation dans AUTRE.
         * Utilisé uniquement quand aucune règle de catégorie principale ne correspond.
         * DIVERS est exclu : c'est la valeur de refus, pas une suggestion automatique.
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
            )
        )
    }
}
