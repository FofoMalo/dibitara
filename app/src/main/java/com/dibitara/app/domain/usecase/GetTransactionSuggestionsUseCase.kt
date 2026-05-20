package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.TransactionSuggestion
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Retourne les suggestions de saisie rapide basées sur l'historique récent.
 *
 * Une suggestion est générée quand le même groupe
 * (libellé normalisé + montant + catégorie + devise + type)
 * apparaît au moins [SEUIL_FREQUENCE] fois dans les 30 derniers jours.
 *
 * Les résultats sont triés par fréquence décroissante et limités à [MAX_SUGGESTIONS].
 * Les transactions récurrentes (gérées automatiquement) et celles sans libellé sont exclues.
 */
class GetTransactionSuggestionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<TransactionSuggestion>> {
        val today = LocalDate.now()
        // Fenêtre glissante de 30 jours (aujourd'hui inclus)
        val from = today.minusDays(29)

        return repository.getByDateRange(from, today).map { transactions ->
            transactions
                .filter { !it.isRecurring && it.note.isNotBlank() }
                .groupBy { tx ->
                    GroupKey(
                        label              = tx.note.trim().lowercase(),
                        amountCents        = tx.amountCents,
                        currency           = tx.currency,
                        category           = tx.category,
                        type               = tx.type,
                        subCategory        = tx.subCategory,
                        customSubCategoryId = tx.customSubCategoryId
                    )
                }
                .filter { (_, groupe) -> groupe.size >= SEUIL_FREQUENCE }
                .map { (cle, groupe) ->
                    // On conserve la casse originale du premier enregistrement du groupe
                    TransactionSuggestion(
                        label               = groupe.first().note.trim(),
                        amountCents         = cle.amountCents,
                        currency            = cle.currency,
                        category            = cle.category,
                        type                = cle.type,
                        subCategory         = cle.subCategory,
                        customSubCategoryId = cle.customSubCategoryId,
                        frequence           = groupe.size
                    )
                }
                .sortedByDescending { it.frequence }
                .take(MAX_SUGGESTIONS)
        }
    }

    private companion object {
        // Nombre minimum d'occurrences pour qu'une transaction devienne une suggestion
        const val SEUIL_FREQUENCE = 2
        // Nombre maximum de suggestions affichées à l'utilisateur
        const val MAX_SUGGESTIONS = 5
    }

    // Clé de regroupement — interne au UseCase, pas un concept du domaine
    private data class GroupKey(
        val label: String,
        val amountCents: Long,
        val currency: Currency,
        val category: Category,
        val type: TransactionType,
        val subCategory: SubCategory?,
        val customSubCategoryId: Long?
    )
}
