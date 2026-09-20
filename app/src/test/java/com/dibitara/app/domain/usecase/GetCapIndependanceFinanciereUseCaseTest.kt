package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetCapIndependanceFinanciereUseCaseTest {

    private val getSpendingRecommendations: GetSpendingRecommendationsUseCase = mockk()
    private val analyserPatrimoine        : AnalyserPatrimoineUseCase         = mockk()
    private val getPatrimoineHistory      : GetPatrimoineHistoryUseCase      = mockk()
    private val prefsRepo                 : UserPreferencesRepository        = mockk()

    private val useCase = GetCapIndependanceFinanciereUseCase(
        getSpendingRecommendations = getSpendingRecommendations,
        analyserPatrimoine         = analyserPatrimoine,
        getPatrimoineHistory       = getPatrimoineHistory,
        userPreferencesRepo        = prefsRepo
    )

    @BeforeEach
    fun setUp() {
        // Dépense lissée 2 000 €/mois → 24 000 €/an, multiple 25× → cap 600 000 €
        every { getSpendingRecommendations(any(), any()) } returns flowOf(
            buildRecommendation(depensesMoyennesCents = 200_000L)
        )
        every { analyserPatrimoine(any(), any()) } returns flowOf(
            buildConseil(versementsProgrammesCents = 0L)
        )
        every { getPatrimoineHistory() } returns flowOf(
            listOf(buildSnapshot(netCents = 150_000_00L))
        )
        every { prefsRepo.get() } returns flowOf(UserPreferences(multipleFICible = 25, rendementFIEsperePct = 0))
    }

    @Test
    fun `capital cible est la dépense annuelle lissée fois le multiple`() = runTest {
        val result = useCase(9, 2026).first()

        assertNotNull(result)
        assertEquals(24_000_00L, result!!.depenseAnnuelleLisseeCents)
        assertEquals(600_000_00L, result.capitalCibleCents)
    }

    @Test
    fun `progression reflète le patrimoine net actuel sur le capital cible`() = runTest {
        // 150 000 € atteints sur 600 000 € cible = 25 %
        val result = useCase(9, 2026).first()

        assertEquals(25, result!!.progressionPct)
        assertEquals(150_000_00L, result.patrimoineNetCents)
    }

    @Test
    fun `progression peut dépasser 100 pourcent quand le cap est déjà atteint`() = runTest {
        every { getPatrimoineHistory() } returns flowOf(listOf(buildSnapshot(netCents = 900_000_00L)))

        val result = useCase(9, 2026).first()

        assertEquals(150, result!!.progressionPct)
        assertEquals(0, result.moisRestantsEstimes)
    }

    @Test
    fun `aucun snapshot connu - résultat nul plutôt qu'un calcul sur une valeur à zéro`() = runTest {
        every { getPatrimoineHistory() } returns flowOf(emptyList())

        val result = useCase(9, 2026).first()

        assertNull(result)
    }

    @Test
    fun `mois restants estimés suit le versement mensuel et le rendement composé`() = runTest {
        // Cap 600 000 €, patrimoine 150 000 € (donc 450 000 € à trouver), versement 1 000 €/mois,
        // rendement nul → 450 mois pile (450 000 / 1 000), sans intérêt composé pour un calcul simple.
        every { analyserPatrimoine(any(), any()) } returns flowOf(
            buildConseil(versementsProgrammesCents = 100_000L)
        )

        val result = useCase(9, 2026).first()

        assertEquals(450, result!!.moisRestantsEstimes)
    }

    @Test
    fun `non atteignable au rythme actuel retourne null plutôt qu'une durée fausse`() = runTest {
        // Aucun versement, aucun rendement : le capital n'augmente jamais.
        every { analyserPatrimoine(any(), any()) } returns flowOf(
            buildConseil(versementsProgrammesCents = 0L)
        )
        every { prefsRepo.get() } returns flowOf(UserPreferences(multipleFICible = 25, rendementFIEsperePct = 0))

        val result = useCase(9, 2026).first()

        assertNull(result!!.moisRestantsEstimes)
    }

    private fun buildRecommendation(depensesMoyennesCents: Long) = SpendingRecommendation(
        currency              = Currency.EUR,
        revenuMoyenCents      = 0L,
        revenuParMoisCents    = emptyList(),
        depensesMoyennesCents = depensesMoyennesCents,
        engagementsMensuels   = 0L,
        tauxEpargneActuelPct  = null,
        tauxEpargneCiblePct   = 20,
        objectifEpargneCents  = 0L,
        pouchesRecommandees   = emptyList(),
        soldePrevisionelCents = 0L,
        estEquilibre          = true,
        moisDeReference       = listOf(8 to 2026, 7 to 2026, 6 to 2026)
    )

    private fun buildConseil(versementsProgrammesCents: Long) = ConseilPatrimoineResult(
        currency                       = Currency.EUR,
        liquiditesSuresCents           = 0L,
        objectifPrecautionCents        = 0L,
        precautionSuffisante           = true,
        revenuMoyenCents               = 0L,
        chargesMensuellesCents         = 0L,
        pochesAvecMarge                = emptyList(),
        objectifEpargneMensuelCents    = 0L,
        resteAVivreReelCents           = 0L,
        versementsProgrammesCents      = versementsProgrammesCents,
        capaciteNonAffecteeCents       = 0L,
        objectifPlafonneParResteAVivre = false,
        repartitionParCategorie        = emptyList(),
        categorieSurConcentree         = null
    )

    private fun buildSnapshot(netCents: Long) = PatrimoineSnapshot(
        id                  = 1L,
        snapshotDate        = LocalDate.of(2026, 8, 15),
        patrimoineBrutCents = netCents,
        patrimoineNetCents  = netCents,
        currency            = Currency.EUR
    )
}
