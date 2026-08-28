package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject

/**
 * Catégorie principale suggérée pour le libellé d'une transaction importée.
 *
 * Même ordre de priorité que [GetRecategorizationSuggestionsUseCase] :
 *  1. règle apprise par l'utilisateur ([CategorizationRuleRepository]) ;
 *  2. dictionnaire de mots-clés générique ([CategoriseurLibelle]) ;
 *  3. à défaut, [Category.AUTRE].
 *
 * Volontairement plus simple que la recatégorisation du Dashboard : pas de
 * sous-catégorie, pas de mot-clé remonté. L'aperçu d'import laisse l'utilisateur
 * ajuster chaque ligne, et le flux « À catégoriser » prend ensuite le relais.
 *
 * Si la logique de priorité 1→2 devait évoluer des deux côtés, extraire un
 * helper partagé depuis `GetRecategorizationSuggestionsUseCase.trouverSuggestion`.
 */
class SuggererCategorieImportUseCase @Inject constructor(
    private val ruleRepository: CategorizationRuleRepository
) {
    suspend operator fun invoke(note: String): Category {
        if (note.isBlank()) return Category.AUTRE

        val regle = ruleRepository.getRuleForNote(note)
        // Une règle « AUTRE » n'est ignorée que si elle ne porte AUCUNE sous-catégorisation :
        // ni [SubCategory] fixe, ni sous-catégorie personnalisée ([customSubCategoryId]).
        // Oublier `customSubCategoryId` ici, c'est le piège documenté dans CLAUDE.md
        // (« Category.AUTRE seul ≠ non catégorisé ») : une règle « AUTRE + sous-catégorie
        // perso » est un vrai choix de l'utilisateur, sa catégorie principale doit être
        // respectée plutôt que redevinée par le dictionnaire de mots-clés.
        val regleSansCategorisation = regle != null &&
            regle.category == Category.AUTRE &&
            regle.subCategory == null &&
            regle.customSubCategoryId == null
        if (regle != null && !regleSansCategorisation) {
            return regle.category
        }

        return CategoriseurLibelle.suggererCategorie(note) ?: Category.AUTRE
    }
}
