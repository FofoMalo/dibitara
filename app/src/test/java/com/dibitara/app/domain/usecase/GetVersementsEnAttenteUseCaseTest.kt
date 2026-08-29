package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetVersementsEnAttenteUseCaseTest {

    private val useCase = GetVersementsEnAttenteUseCase()

    private fun compte(id: Long, contribution: Long) = SavingsAccount(
        id = id, type = SavingsType.LIVRET_A, label = "Compte $id",
        currentBalanceCents = 100_000L, monthlyContributionCents = contribution,
        currency = Currency.EUR, childId = null, updatedAt = LocalDate.now()
    )

    private fun versement(accountId: Long) = MonthlyVersement(
        id = 0L, accountId = accountId, compteType = CompteType.EPARGNE,
        year = 2026, month = 8, montantCents = 20_000L, currency = Currency.EUR
    )

    @Test
    fun `compte avec versement mensuel prévu et non versé - en attente`() {
        val result = useCase(listOf(compte(1L, 20_000L)), emptyList())
        assertEquals(listOf(1L), result)
    }

    @Test
    fun `compte déjà versé ce mois-ci - pas en attente`() {
        val result = useCase(listOf(compte(1L, 20_000L)), listOf(versement(1L)))
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `compte sans versement mensuel - jamais en attente`() {
        val result = useCase(listOf(compte(1L, 0L)), emptyList())
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `aucun compte - liste vide`() {
        assertEquals(emptyList<Long>(), useCase(emptyList(), emptyList()))
    }

    @Test
    fun `mix - seuls les comptes prévus et non versés remontent`() {
        val comptes = listOf(
            compte(1L, 20_000L),   // prévu, non versé → en attente
            compte(2L, 30_000L),   // prévu, versé → non
            compte(3L, 0L),        // pas de versement mensuel → non
            compte(4L, 15_000L)    // prévu, non versé → en attente
        )
        val result = useCase(comptes, listOf(versement(2L)))
        assertEquals(listOf(1L, 4L), result)
    }
}
