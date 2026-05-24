package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.RecategorizationSuggestion
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

        for (regle in REGLES_MOTS_CLES) {
            val motCorrespondant = regle.motsCles.firstOrNull { libelle.contains(it) }
            if (motCorrespondant != null) {
                return RecategorizationSuggestion(
                    transaction       = transaction,
                    suggestedCategory = regle.categorie,
                    matchedKeyword    = motCorrespondant
                )
            }
        }
        return null
    }

    // Règle associant une liste de mots-clés à la catégorie à proposer
    private data class RegleCategorisation(val motsCles: List<String>, val categorie: Category)

    companion object {
        /**
         * Dictionnaire de règles de recatégorisation.
         * Les mots-clés sont comparés en minuscules contre [Transaction.note].
         * L'ordre des règles importe : la première correspondance l'emporte.
         */
        private val REGLES_MOTS_CLES = listOf(
            RegleCategorisation(
                listOf("leclerc", "carrefour", "lidl", "aldi", "intermarché", "casino", "monoprix",
                    "franprix", "picard", "biocoop", "marché", "epicerie", "supermarché", "boulangerie"),
                Category.ALIMENTATION
            ),
            RegleCategorisation(
                listOf("loyer", "charges", "syndic", "copropriété", "électricité", "edf", "engie",
                    "gaz", "eau", "veolia", "suez", "orange money home", "internet", "fibre"),
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
                    "sport", "loisirs", "fnac", "culture", "concert", "festival"),
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
}
