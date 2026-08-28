package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject

/**
 * Catégorisation suggérée pour le libellé d'une transaction importée depuis un CSV.
 *
 * Délègue toute la logique de priorité à [CascadeCategorisation] (règle apprise →
 * dictionnaire de catégorie principale → dictionnaire de sous-catégorie d'`AUTRE`),
 * partagée avec la recatégorisation du Dashboard pour éviter deux implémentations
 * qui divergeraient.
 *
 * Contrairement au Dashboard, l'import récupère aussi [SuggestionCategorieImport.customSubCategoryId] :
 * si l'utilisateur a appris « ce libellé → sous-catégorie perso X », un ré-import
 * (ou le mois suivant) applique X directement. L'aperçu reste éditable ligne à ligne.
 */
class SuggererCategorieImportUseCase @Inject constructor(
    private val ruleRepository: CategorizationRuleRepository
) {
    suspend operator fun invoke(note: String): SuggestionCategorieImport {
        val suggestion = CascadeCategorisation.suggerer(note, ruleRepository)
            ?: return SuggestionCategorieImport(Category.AUTRE)
        return SuggestionCategorieImport(
            category = suggestion.category,
            subCategory = suggestion.subCategory,
            customSubCategoryId = suggestion.customSubCategoryId,
        )
    }
}

/** Catégorie principale + éventuelle sous-catégorie (fixe ou personnalisée) pour une ligne importée. */
data class SuggestionCategorieImport(
    val category: Category,
    val subCategory: SubCategory? = null,
    val customSubCategoryId: Long? = null,
)
