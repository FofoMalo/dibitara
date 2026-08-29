package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsGoal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Projection de la date d'atteinte d'un objectif d'épargne, au rythme de son
 * versement mensuel.
 *
 * UseCase PUR (pas de repository, `@Inject constructor()` sans dépendance) - même
 * forme que [CalculerTendanceActifUseCase]. La date « aujourd'hui » est injectable
 * pour rendre les tests déterministes.
 *
 * Trois cas où il n'y a rien à projeter (tous les champs à null) :
 *  - l'objectif est déjà atteint (reste ≤ 0) ;
 *  - `targetAmountCents` est nul ou incohérent (garde-fou, la saisie l'interdit) ;
 *  - il n'y a pas de versement mensuel (`monthlyContributionCents <= 0`) - on ne
 *    peut pas dire quand ce sera fini, seul le % a du sens.
 */
class ProjeterObjectifUseCase @Inject constructor() {

    operator fun invoke(goal: SavingsGoal, aujourdhui: LocalDate = LocalDate.now()): ProjectionObjectif {
        val reste = goal.targetAmountCents - goal.currentAmountCents
        if (reste <= 0L || goal.monthlyContributionCents <= 0L) {
            return ProjectionObjectif(null, null, null, null)
        }

        // ceil : s'il reste 250 € et qu'on verse 100 €/mois, il faut 3 mois (pas 2,5).
        val moisRestants = ceil(reste.toDouble() / goal.monthlyContributionCents.toDouble()).toInt()
        val dateProjetee = aujourdhui.plusMonths(moisRestants.toLong())

        // Écart en mois entre le 1er du mois cible et le 1er du mois projeté (négatif = en avance).
        val ecartMois = ChronoUnit.MONTHS.between(
            goal.targetDate.withDayOfMonth(1),
            dateProjetee.withDayOfMonth(1)
        ).toInt()

        return ProjectionObjectif(
            moisRestants = moisRestants,
            dateProjetee = dateProjetee,
            ecartMois    = ecartMois,
            tenable      = !dateProjetee.isAfter(goal.targetDate)
        )
    }
}

/**
 * @param moisRestants nombre de versements mensuels avant l'atteinte ; null si non calculable.
 * @param dateProjetee date d'atteinte estimée ; null si non calculable.
 * @param ecartMois    dateProjetee − targetDate en mois ; < 0 = en avance ; null si non calculable.
 * @param tenable      true si la date projetée respecte l'échéance ; null si non calculable.
 */
data class ProjectionObjectif(
    val moisRestants: Int?,
    val dateProjetee: LocalDate?,
    val ecartMois: Int?,
    val tenable: Boolean?
)
