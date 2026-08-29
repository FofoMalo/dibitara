package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsGoal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetObjectifsVersementEnAttenteUseCaseTest {

    private val useCase = GetObjectifsVersementEnAttenteUseCase()

    private fun objectif(id: Long, mensualite: Long) = SavingsGoal(
        id = id, name = "Objectif $id", targetAmountCents = 1_000_000L, currentAmountCents = 0L,
        targetDate = LocalDate.now().plusMonths(12), monthlyContributionCents = mensualite,
        currency = Currency.EUR, colorKey = GoalColor.OR, iconKey = GoalIcon.AUTRE
    )

    private fun versement(goalId: Long) = MonthlyVersement(
        id = 0L, accountId = goalId, compteType = CompteType.OBJECTIF,
        year = 2026, month = 8, montantCents = 40_000L, currency = Currency.EUR
    )

    @Test
    fun `objectif avec mensualité non versée ce mois-ci - en attente`() {
        assertEquals(listOf(1L), useCase(listOf(objectif(1L, 40_000L)), emptyList()))
    }

    @Test
    fun `objectif déjà versé ce mois-ci - pas en attente`() {
        assertEquals(emptyList<Long>(), useCase(listOf(objectif(1L, 40_000L)), listOf(versement(1L))))
    }

    @Test
    fun `objectif sans mensualité - jamais en attente`() {
        assertEquals(emptyList<Long>(), useCase(listOf(objectif(1L, 0L)), emptyList()))
    }

    @Test
    fun `objectif atteint - jamais en attente (pas de nag sur un objectif fini)`() {
        val atteint = objectif(1L, 40_000L).copy(
            targetAmountCents = 1_000_000L, currentAmountCents = 1_000_000L
        )
        assertEquals(emptyList<Long>(), useCase(listOf(atteint), emptyList()))
    }

    @Test
    fun `mix`() {
        val objectifs = listOf(objectif(1L, 40_000L), objectif(2L, 30_000L), objectif(3L, 0L))
        assertEquals(listOf(1L), useCase(objectifs, listOf(versement(2L))))
    }
}
