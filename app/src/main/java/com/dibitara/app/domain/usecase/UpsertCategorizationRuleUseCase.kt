package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject

/**
 * Sauvegarde une règle de catégorisation à partir d'un choix manuel de l'utilisateur.
 *
 * N'enregistre rien si :
 * - le libellé est vide (on ne peut pas matcher sur une note vide)
 * - la transaction est un revenu (les revenus sont toujours en AUTRE par design)
 * - la catégorie est AUTRE sans aucune sous-catégorie (la transaction n'est pas encore catégorisée)
 */
class UpsertCategorizationRuleUseCase @Inject constructor(
    private val repository: CategorizationRuleRepository
) {
    suspend operator fun invoke(
        note: String,
        type: TransactionType,
        category: Category,
        subCategory: SubCategory? = null,
        customSubCategoryId: Long? = null
    ) {
        if (note.isBlank()) return
        if (type != TransactionType.EXPENSE) return
        if (category == Category.AUTRE && subCategory == null && customSubCategoryId == null) return

        repository.upsert(
            CategorizationRule(
                noteExact            = note.trim().lowercase(),
                category             = category,
                subCategory          = subCategory,
                customSubCategoryId  = customSubCategoryId
            )
        )
    }
}
