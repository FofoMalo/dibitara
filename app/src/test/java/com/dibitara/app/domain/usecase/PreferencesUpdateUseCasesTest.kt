package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour les UseCases de mise à jour des préférences utilisateur :
 * - UpdateDashboardCardOrderUseCase
 * - UpdateNotificationsMensuellesUseCase
 *
 * Ces UseCases sont de simples délégués vers le repository.
 * On vérifie qu'ils transmettent bien l'argument reçu.
 */
class PreferencesUpdateUseCasesTest {

    private val repository: UserPreferencesRepository = mockk()

    // ─── UpdateDashboardCardOrder ─────────────────────────────────────────────

    @Nested
    inner class UpdateDashboardCardOrderTest {

        private lateinit var useCase: UpdateDashboardCardOrderUseCase

        @BeforeEach
        fun setUp() {
            useCase = UpdateDashboardCardOrderUseCase(repository)
        }

        @Test
        fun `délègue l'ordre complet au repository`() = runTest {
            val ordre = DashboardCard.entries.toList()
            coJustRun { repository.updateDashboardCardOrder(ordre) }

            useCase(ordre)

            coVerify(exactly = 1) { repository.updateDashboardCardOrder(ordre) }
        }

        @Test
        fun `délègue un ordre personnalisé au repository`() = runTest {
            // L'utilisateur a déplacé DETTES en tête
            val ordre = listOf(
                DashboardCard.DETTES,
                DashboardCard.CASHFLOW_PROJECTION,
                DashboardCard.METRIQUES_BUDGET_EPARGNE,
                DashboardCard.METRIQUES_INVESTISSEMENTS,
                DashboardCard.RAPPORT_GRAPHIQUE,
                DashboardCard.SUGGESTIONS_RECATEGORISATION,
                DashboardCard.PROCHAINS_PAIEMENTS
            )
            coJustRun { repository.updateDashboardCardOrder(ordre) }

            useCase(ordre)

            coVerify(exactly = 1) { repository.updateDashboardCardOrder(ordre) }
        }

        @Test
        fun `délègue une liste vide au repository`() = runTest {
            // Cas limite : liste vide (ne devrait pas arriver en pratique, mais le UseCase ne valide pas)
            coJustRun { repository.updateDashboardCardOrder(emptyList()) }

            useCase(emptyList())

            coVerify(exactly = 1) { repository.updateDashboardCardOrder(emptyList()) }
        }
    }

    // ─── UpdateNotificationsMensuelles ───────────────────────────────────────

    @Nested
    inner class UpdateNotificationsMensuellesTest {

        private lateinit var useCase: UpdateNotificationsMensuellesUseCase

        @BeforeEach
        fun setUp() {
            useCase = UpdateNotificationsMensuellesUseCase(repository)
        }

        @Test
        fun `délègue l'activation au repository`() = runTest {
            coJustRun { repository.updateNotificationsMensuelles(true) }

            useCase(true)

            coVerify(exactly = 1) { repository.updateNotificationsMensuelles(true) }
        }

        @Test
        fun `délègue la désactivation au repository`() = runTest {
            coJustRun { repository.updateNotificationsMensuelles(false) }

            useCase(false)

            coVerify(exactly = 1) { repository.updateNotificationsMensuelles(false) }
        }
    }
}
