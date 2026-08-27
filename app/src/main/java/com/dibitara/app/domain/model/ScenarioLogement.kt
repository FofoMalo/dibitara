package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Résultat du simulateur "Scénario logement" : peut-on garder un bien immobilier
 * (typiquement la résidence principale) avec un revenu simulé (ex : changement de travail) ?
 *
 * [mensualiteMaisonCents]       : mensualité du crédit lié au bien, purement informative -
 *                                 null si aucun crédit n'est lié (`RealEstateAsset.debtId`).
 *                                 Volontairement PAS additionnée dans [revenuPlancherCents] :
 *                                 si elle est payée en pratique, elle apparaît déjà dans les
 *                                 dépenses réelles catégorisées LOGEMENT (bucket BESOINS), donc
 *                                 l'additionner en plus la compterait deux fois.
 * [autresEngagementsCents]      : autres dettes (hors crédit du bien) + versements épargne programmés.
 * [besoinsIncompressiblesCents] : moyenne réelle des dépenses BESOINS sur les 3 derniers mois
 *                                 (inclut LOGEMENT, donc la mensualité si elle y est catégorisée).
 * [revenuPlancherCents]         : autresEngagementsCents + besoinsIncompressiblesCents - le revenu
 *                                 minimum pour tenir, à comparer directement à une offre d'emploi.
 * [seuilResteAVivreCents]       : marge de sécurité minimale visée (UserPreferences.seuilFondsCents),
 *                                 utilisée comme repère de viabilité plutôt que zéro strict.
 * [estTenable]                  : resteAVivreSimuleCents >= seuilResteAVivreCents.
 * [pochesEnviesReduction]       : poches "Envies" suggérées en réduction si le scénario est tendu
 *                                 (voir [PocheReduction]), vide si le scénario est déjà tenable.
 * [ecartNonCouvertCents]        : manquant restant même après les réductions suggérées (0 si tenable
 *                                 ou entièrement couvert) - dit honnêtement quand le plan ne suffit pas.
 */
data class CapaciteLogementResult(
    val currency                    : Currency,
    val bienLabel                   : String,
    val mensualiteMaisonCents       : Long?,
    val autresEngagementsCents      : Long,
    val besoinsIncompressiblesCents : Long,
    val revenuPlancherCents         : Long,
    val revenuReelMoyenCents        : Long,
    val revenuSimuleCents           : Long,
    val resteAVivreReelCents        : Long,
    val resteAVivreSimuleCents      : Long,
    val seuilResteAVivreCents       : Long,
    val estTenable                  : Boolean,
    val pochesEnviesReduction       : List<PocheReduction>,
    val ecartNonCouvertCents        : Long
)

/**
 * Suggestion de réduction sur une poche "Envies" pour combler un scénario tendu.
 * [montantACouperCents] est plafonné à 50 % de [moyenneCents] - on ne suggère jamais de
 * couper une poche à zéro, une "solution" que personne n'applique en pratique.
 */
data class PocheReduction(
    val category           : Category,
    val moyenneCents        : Long,
    val montantACouperCents : Long
)

/**
 * Projection du reste à vivre cumulé sur plusieurs mois sous le scénario simulé.
 *
 * Le revenu simulé et les besoins incompressibles sont tenus constants mois après mois,
 * mais deux évènements réels changent la trajectoire :
 *  - le remboursement d'une dette (hors crédit du bien) qui se termine dans la fenêtre
 *    (approximé par totalCents / monthlyPaymentCents, comme l'affichage existant de DebtsScreen) ;
 *  - une charge annuelle récurrente (assurance, impôts...) dont la prochaine échéance tombe
 *    dans la fenêtre.
 *
 * [points] : un point par mois, [CashflowPoint.soldeCents] est le cumul (pas un solde bancaire).
 * [moisPassageNegatif] : premier mois où le cumul devient négatif, null si aucun risque sur l'horizon.
 */
data class ScenarioLogementProjection(
    val points            : List<CashflowPoint>,
    val currency          : Currency,
    val moisPassageNegatif: LocalDate?
)
