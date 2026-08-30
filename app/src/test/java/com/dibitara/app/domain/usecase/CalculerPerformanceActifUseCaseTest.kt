package com.dibitara.app.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CalculerPerformanceActifUseCaseTest {

    private val useCase = CalculerPerformanceActifUseCase()

    @Test
    fun `valeur d'acquisition nulle - retourne null (évite la division par zéro)`() {
        assertNull(useCase(0L, 100_000_00L))
    }

    @Test
    fun `hausse sans versement - delta et pourcentage calculés correctement`() {
        val result = useCase(200_000_00L, 220_000_00L)!!
        assertEquals(20_000_00L, result.deltaCents)
        assertEquals(10f, result.deltaPct, 0.01f)
    }

    @Test
    fun `baisse sans versement - delta et pourcentage négatifs`() {
        val result = useCase(200_000_00L, 180_000_00L)!!
        assertEquals(-20_000_00L, result.deltaCents)
        assertEquals(-10f, result.deltaPct, 0.01f)
    }

    @Test
    fun `versements cumulés neutralisés - performance nette de l'apport`() {
        // SCPI acquise à 4 200€, valant 4 580€ aujourd'hui, avec 600€ de versements
        // entre-temps : la vraie performance est de 4 580 - 4 200 - 600 = -220€, pas +380€.
        val result = useCase(4_200_00L, 4_580_00L, versementsCumulesCents = 600_00L)!!
        assertEquals(-220_00L, result.deltaCents)
        assertEquals((-220_00L.toFloat() / 4_200_00L.toFloat()) * 100f, result.deltaPct, 0.01f)
    }

    @Test
    fun `retrait de capital neutralisé - la baisse du montant n'est pas comptée en perte`() {
        // Actif libre (CTO) acquis à 10 000€, toujours valant 10 000€ après un retrait de
        // 1 000€ (versementsCumulesCents négatif) : la vraie performance est nulle, pas -10%.
        val result = useCase(10_000_00L, 9_000_00L, versementsCumulesCents = -1_000_00L)!!
        assertEquals(0L, result.deltaCents)
        assertEquals(0f, result.deltaPct, 0.01f)
    }
}
