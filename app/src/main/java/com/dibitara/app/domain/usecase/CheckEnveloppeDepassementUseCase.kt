package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EnveloppeStatus
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule les enveloppes dont le taux de dépense dépasse [seuilTaux] pour le mois courant.
 * Utilisé par [AppViewModel] au démarrage pour décider d'envoyer ou non une notification.
 *
 * @param seuilTaux 0.8 = 80 %, seuil d'alerte orange ; 1.0 = plafond atteint.
 */
class CheckEnveloppeDepassementUseCase @Inject constructor(
    private val enveloppeRepository  : CategoryEnvelopeRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(seuilTaux: Float = 0.8f): List<EnveloppeStatus> {
        val today      = LocalDate.now()
        val enveloppes = enveloppeRepository.getAll().first()
        if (enveloppes.isEmpty()) return emptyList()

        val transactions = transactionRepository.getByMonth(today.monthValue, today.year).first()
        val depenseParCategorie = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }

        return enveloppes.mapNotNull { env ->
            val depense = depenseParCategorie[env.category] ?: 0L
            val taux    = if (env.plafondCents > 0) depense.toFloat() / env.plafondCents else 0f
            if (taux >= seuilTaux) {
                EnveloppeStatus(envelope = env, depenseCents = depense, taux = taux)
            } else null
        }
    }
}
