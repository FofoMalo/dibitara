package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategorizationRuleRepository
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
 * Une transaction avec [Transaction.subCategory] ou [Transaction.customSubCategoryId] déjà
 * renseigné est exclue : cela signifie que l'utilisateur a déjà statué (refus via
 * SubCategory.DIVERS, sous-catégorie personnalisée ou autre choix manuel).
 *
 * La logique de priorité (règle apprise → dictionnaire principal → sous-catégorie)
 * vit dans [CascadeCategorisation], partagée avec l'import CSV.
 *
 * [today] est injectable pour les tests.
 */
class GetRecategorizationSuggestionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: CategorizationRuleRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<List<RecategorizationSuggestion>> {
        val debut = today.minusDays(90)
        return transactionRepository.getByDateRange(debut, today).map { transactions ->
            transactions
                // Les revenus ne peuvent pas être recatégorisés depuis l'UI (champs masqués)
                .filter {
                    it.category == Category.AUTRE && it.type == TransactionType.EXPENSE &&
                        it.subCategory == null && it.customSubCategoryId == null
                }
                .mapNotNull { trouverSuggestion(it) }
                .distinctBy { it.transaction.id }
        }
    }

    private suspend fun trouverSuggestion(transaction: Transaction): RecategorizationSuggestion? {
        val suggestion = CascadeCategorisation.suggerer(transaction.note, ruleRepository) ?: return null
        // Le Dashboard ne sait pas proposer une sous-catégorie personnalisée
        // ([RecategorizationSuggestion] ne porte pas customSubCategoryId) : une
        // suggestion qui ne change ni la catégorie principale ni la SubCategory
        // fixe serait un no-op à l'écran.
        if (suggestion.category == Category.AUTRE && suggestion.subCategory == null) return null
        return RecategorizationSuggestion(
            transaction          = transaction,
            suggestedCategory    = suggestion.category,
            matchedKeyword       = suggestion.motCle ?: "",
            suggestedSubCategory = suggestion.subCategory
        )
    }
}
