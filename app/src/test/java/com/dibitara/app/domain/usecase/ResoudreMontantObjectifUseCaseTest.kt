package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.FundingMode
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.model.SavingsType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.math.roundToLong

class ResoudreMontantObjectifUseCaseTest {

    private val useCase = ResoudreMontantObjectifUseCase()
    private val rates = ExchangeRates(usdParEur = 1.09, xofParEur = 655.96, horodatage = 0L)

    private fun goal(
        currentAmountCents: Long,
        currency: Currency = Currency.EUR,
        fundingMode: FundingMode? = null,
        sourceAccountId: Long? = null
    ) = SavingsGoal(
        id = 1L, name = "Voiture", targetAmountCents = 1_000_000L,
        currentAmountCents = currentAmountCents, targetDate = LocalDate.now(),
        monthlyContributionCents = 0L, currency = currency,
        colorKey = GoalColor.OR, iconKey = GoalIcon.VOITURE,
        sourceAccountId = sourceAccountId, fundingMode = fundingMode
    )

    private fun compte(id: Long, balanceCents: Long, currency: Currency = Currency.EUR) = SavingsAccount(
        id = id, type = SavingsType.LIVRET_A, label = "Livret", currentBalanceCents = balanceCents,
        monthlyContributionCents = 0L, currency = currency, updatedAt = LocalDate.now()
    )

    @Test
    fun `mode MANUEL ou non renseigne retourne le montant stocke tel quel`() {
        val g = goal(currentAmountCents = 42_000L, fundingMode = FundingMode.MANUEL)
        assertEquals(42_000L, useCase(g, comptes = emptyList(), rates = rates))

        val gNull = goal(currentAmountCents = 42_000L, fundingMode = null)
        assertEquals(42_000L, useCase(gNull, comptes = emptyList(), rates = rates))
    }

    @Test
    fun `mode VERSEMENTS retourne le montant stocke tel quel - rien a resoudre`() {
        val g = goal(currentAmountCents = 128_000L, fundingMode = FundingMode.VERSEMENTS)
        assertEquals(128_000L, useCase(g, comptes = emptyList(), rates = rates))
    }

    @Test
    fun `mode SOLDE_COMPTE meme devise retourne le solde du compte lie`() {
        val g = goal(currentAmountCents = 0L, fundingMode = FundingMode.SOLDE_COMPTE, sourceAccountId = 7L)
        val comptes = listOf(compte(id = 7L, balanceCents = 350_000L))
        assertEquals(350_000L, useCase(g, comptes, rates))
    }

    @Test
    fun `mode SOLDE_COMPTE objectif XOF et compte EUR convertit`() {
        // Cas réel du cadrage : objectif "Projet Burkina" en XOF, compte source en EUR.
        val g = goal(currentAmountCents = 0L, currency = Currency.XOF, fundingMode = FundingMode.SOLDE_COMPTE, sourceAccountId = 7L)
        val comptes = listOf(compte(id = 7L, balanceCents = 10_000L, currency = Currency.EUR)) // 100,00 €
        val attendu = (10_000L * rates.xofParEur).roundToLong()
        assertEquals(attendu, useCase(g, comptes, rates))
    }

    @Test
    fun `mode SOLDE_COMPTE compte introuvable retombe sur la valeur stockee`() {
        val g = goal(currentAmountCents = 99_000L, fundingMode = FundingMode.SOLDE_COMPTE, sourceAccountId = 404L)
        assertEquals(99_000L, useCase(g, comptes = emptyList(), rates = rates))
    }
}
