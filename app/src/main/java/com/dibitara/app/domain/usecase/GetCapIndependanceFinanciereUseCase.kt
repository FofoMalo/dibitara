package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CapIndependanceFinanciere
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule le cap vers l'indépendance financière (F1/F2, CADRAGE_INDEPENDANCE_FINANCIERE.md).
 *
 * Ne réimplémente rien : réutilise [GetSpendingRecommendationsUseCase] pour la dépense
 * lissée, [AnalyserPatrimoineUseCase] pour les versements déjà programmés et
 * [GetPatrimoineHistoryUseCase] pour le patrimoine net réel. Voir [CapIndependanceFinanciere]
 * pour le détail de chaque champ et les choix de conception.
 */
class GetCapIndependanceFinanciereUseCase @Inject constructor(
    private val getSpendingRecommendations: GetSpendingRecommendationsUseCase,
    private val analyserPatrimoine        : AnalyserPatrimoineUseCase,
    private val getPatrimoineHistory      : GetPatrimoineHistoryUseCase,
    private val userPreferencesRepo       : UserPreferencesRepository
) {
    operator fun invoke(
        refMonth: Int = LocalDate.now().monthValue,
        refYear: Int = LocalDate.now().year
    ): Flow<CapIndependanceFinanciere?> = combine(
        getSpendingRecommendations(refMonth, refYear),
        analyserPatrimoine(refMonth, refYear),
        getPatrimoineHistory(),
        userPreferencesRepo.get()
    ) { recommandation, conseil, historique, prefs ->
        // Aucun snapshot connu : rien à mesurer tant que l'écran Patrimoine n'a jamais été
        // ouvert (le premier snapshot y est créé automatiquement).
        val patrimoineNetCents = historique.lastOrNull()?.patrimoineNetCents ?: return@combine null

        val depenseAnnuelleLisseeCents = recommandation.depensesMoyennesCents * 12
        val capitalCibleCents = depenseAnnuelleLisseeCents * prefs.multipleFICible
        val progressionPct = if (capitalCibleCents > 0) {
            (patrimoineNetCents * 100 / capitalCibleCents).toInt()
        } else 0

        CapIndependanceFinanciere(
            currency                  = recommandation.currency,
            depenseAnnuelleLisseeCents = depenseAnnuelleLisseeCents,
            capitalCibleCents         = capitalCibleCents,
            multipleCible             = prefs.multipleFICible,
            patrimoineNetCents        = patrimoineNetCents,
            progressionPct            = progressionPct,
            versementMensuelCents     = conseil.versementsProgrammesCents,
            rendementEspereAnnuelPct  = prefs.rendementFIEsperePct,
            moisRestantsEstimes       = moisRestantsPourAtteindre(
                capitalActuel      = patrimoineNetCents,
                capitalCible       = capitalCibleCents,
                versementMensuel   = conseil.versementsProgrammesCents,
                rendementAnnuelPct = prefs.rendementFIEsperePct
            )
        )
    }

    /**
     * Simulation mensuelle simple (versement + rendement composé) jusqu'au capital cible.
     * Bornée à [MOIS_MAX] (60 ans) : au-delà, considéré comme non atteignable au rythme
     * actuel plutôt que d'afficher une durée déraisonnable.
     */
    private fun moisRestantsPourAtteindre(
        capitalActuel: Long,
        capitalCible: Long,
        versementMensuel: Long,
        rendementAnnuelPct: Int
    ): Int? {
        if (capitalActuel >= capitalCible) return 0

        val rendementMensuel = rendementAnnuelPct / 100.0 / 12
        var capital = capitalActuel.toDouble()
        var mois = 0
        while (capital < capitalCible && mois < MOIS_MAX) {
            capital = capital * (1 + rendementMensuel) + versementMensuel
            mois++
        }
        return if (mois >= MOIS_MAX) null else mois
    }

    companion object {
        private const val MOIS_MAX = 720 // 60 ans
    }
}
