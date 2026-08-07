package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EnveloppeStatus
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Version réactive de [CheckEnveloppeDepassementUseCase] (même calcul de taux),
 * pour un affichage continu sur le Dashboard plutôt qu'une vérification ponctuelle
 * au démarrage de l'app.
 *
 * @param seuilTaux 0.8 = 80 %, seuil d'alerte orange ; 1.0 = plafond atteint.
 */
class GetEnveloppesEnAlerteUseCase @Inject constructor(
    private val enveloppeRepository  : CategoryEnvelopeRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(seuilTaux: Float = 0.8f): Flow<List<EnveloppeStatus>> {
        val today = LocalDate.now()
        return combine(
            enveloppeRepository.getAll(),
            transactionRepository.getByMonth(today.monthValue, today.year)
        ) { enveloppes, transactions ->
            val depenseParCategorie = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amountCents } }

            enveloppes.mapNotNull { env ->
                val depense = depenseParCategorie[env.category] ?: 0L
                val taux    = if (env.plafondCents > 0) depense.toFloat() / env.plafondCents else 0f
                if (taux >= seuilTaux) EnveloppeStatus(envelope = env, depenseCents = depense, taux = taux) else null
            }
        }
    }
}
