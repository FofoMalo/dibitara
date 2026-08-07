package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Migration ponctuelle : la sous-catégorie personnalisée "Tabac" (créée sous AUTRE)
 * devient la catégorie [Category.TABAC] à part entière, afin de pouvoir lui associer
 * une enveloppe budgétaire - impossible sur une simple sous-catégorie personnalisée.
 *
 * Idempotente : si la sous-catégorie "Tabac" n'existe plus (déjà migrée, ou jamais
 * créée sur cette installation), ne fait rien. Peut donc être appelée à chaque
 * démarrage sans effet de bord, comme les autres vérifications d'[AppViewModel].
 */
class MigrerTabacVersCategorieUseCase @Inject constructor(
    private val customSubCategoryRepository: CustomSubCategoryRepository,
    private val transactionRepository      : TransactionRepository
) {
    suspend operator fun invoke() {
        val tabac = customSubCategoryRepository.getByCategory(Category.AUTRE).first()
            .firstOrNull { it.name.equals("Tabac", ignoreCase = true) }
            ?: return

        transactionRepository.getAll().first()
            .filter { it.category == Category.AUTRE && it.customSubCategoryId == tabac.id }
            .forEach { transaction ->
                transactionRepository.update(
                    transaction.copy(category = Category.TABAC, customSubCategoryId = null)
                )
            }

        customSubCategoryRepository.delete(tabac)
    }
}
