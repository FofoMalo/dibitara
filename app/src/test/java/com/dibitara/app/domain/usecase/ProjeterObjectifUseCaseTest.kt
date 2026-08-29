package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.SavingsGoal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProjeterObjectifUseCaseTest {

    private val useCase = ProjeterObjectifUseCase()
    private val aujourdhui = LocalDate.of(2026, 1, 1)

    private fun goal(
        target: Long,
        current: Long,
        monthly: Long,
        echeance: LocalDate = LocalDate.of(2027, 1, 1)
    ) = SavingsGoal(
        id = 1L, name = "Voiture", targetAmountCents = target, currentAmountCents = current,
        targetDate = echeance, monthlyContributionCents = monthly, currency = Currency.EUR,
        colorKey = GoalColor.OR, iconKey = GoalIcon.VOITURE
    )

    @Test
    fun `objectif atteint - rien à projeter`() {
        val p = useCase(goal(target = 1_000_00L, current = 1_000_00L, monthly = 100_00L), aujourdhui)
        assertNull(p.moisRestants)
        assertNull(p.dateProjetee)
        assertNull(p.tenable)
    }

    @Test
    fun `montant objectif nul - early return, pas de date dans le passé`() {
        val p = useCase(goal(target = 0L, current = 0L, monthly = 100_00L), aujourdhui)
        assertNull(p.moisRestants)
        assertNull(p.dateProjetee)
        assertNull(p.ecartMois)
        assertNull(p.tenable)
    }

    @Test
    fun `pas de versement mensuel - juste le pourcentage, pas de projection`() {
        val p = useCase(goal(target = 1_000_00L, current = 200_00L, monthly = 0L), aujourdhui)
        assertNull(p.moisRestants)
        assertNull(p.dateProjetee)
        assertNull(p.tenable)
    }

    @Test
    fun `en avance - date projetée avant l'échéance, tenable`() {
        // reste 600 €, 200 €/mois → 3 mois → avril 2026, échéance janvier 2027
        val p = useCase(
            goal(target = 1_000_00L, current = 400_00L, monthly = 200_00L,
                echeance = LocalDate.of(2027, 1, 1)),
            aujourdhui
        )
        assertEquals(3, p.moisRestants)
        assertEquals(LocalDate.of(2026, 4, 1), p.dateProjetee)
        assertEquals(true, p.tenable)
        assertTrue(p.ecartMois!! < 0)
    }

    @Test
    fun `en retard - date projetée après l'échéance, non tenable`() {
        // reste 900 €, 100 €/mois → 9 mois → octobre 2026, échéance mars 2026
        val p = useCase(
            goal(target = 1_000_00L, current = 100_00L, monthly = 100_00L,
                echeance = LocalDate.of(2026, 3, 1)),
            aujourdhui
        )
        assertEquals(9, p.moisRestants)
        assertEquals(LocalDate.of(2026, 10, 1), p.dateProjetee)
        assertEquals(false, p.tenable)
        assertEquals(7, p.ecartMois)
    }

    @Test
    fun `reste non multiple de la mensualité - arrondi au mois supérieur (ceil)`() {
        // reste 250 €, 100 €/mois → 3 mois (pas 2)
        val p = useCase(goal(target = 1_000_00L, current = 750_00L, monthly = 100_00L), aujourdhui)
        assertEquals(3, p.moisRestants)
    }

    @Test
    fun `en retard de quelques jours - non tenable et ecartMois vaut 0 (même mois calendaire)`() {
        // aujourd'hui 2026-08-29, reste 100 € à 100 €/mois → 1 mois → projeté 2026-09-29.
        // Échéance 2026-09-15 : dépassée (tenable false) mais même mois → ecartMois = 0.
        // L'UI doit alors écrire « En retard sur l'échéance », pas « En retard de 0 mois ».
        val p = useCase(
            goal(target = 100_00L, current = 0L, monthly = 100_00L,
                echeance = LocalDate.of(2026, 9, 15)),
            LocalDate.of(2026, 8, 29)
        )
        assertEquals(false, p.tenable)
        assertEquals(0, p.ecartMois)
    }

    @Test
    fun `pile à l'échéance - tenable (pas après)`() {
        // reste 1200 €, 100 €/mois → 12 mois → janvier 2027 = échéance
        val p = useCase(
            goal(target = 1_200_00L, current = 0L, monthly = 100_00L,
                echeance = LocalDate.of(2027, 1, 1)),
            aujourdhui
        )
        assertEquals(LocalDate.of(2027, 1, 1), p.dateProjetee)
        assertEquals(true, p.tenable)
        assertEquals(0, p.ecartMois)
    }
}
