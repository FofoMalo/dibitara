package com.dibitara.app.domain.model

/**
 * Cap vers l'indépendance financière (F1/F2 de CADRAGE_INDEPENDANCE_FINANCIERE.md).
 *
 * Choix volontaire : basé sur la trajectoire du patrimoine net réel plutôt que sur le
 * ratio revenu/dépense mensuel (déjà utilisé par [SpendingRecommendation.tauxEpargneActuelPct]) -
 * ce dernier se plafonne à 0 dès qu'un mois a un revenu bas ou sous-capté, ce qui a été
 * constaté en pratique (cf. §1 du cadrage) et rend le cap illisible un mois sur trois.
 *
 * [depenseAnnuelleLisseeCents] : [SpendingRecommendation.depensesMoyennesCents] × 12 - la
 *   dépense annuelle "lissée" sur 3 mois plutôt que le dernier mois, qui peut être un accident.
 * [capitalCibleCents]      : [depenseAnnuelleLisseeCents] × [multipleCible].
 * [multipleCible]          : [UserPreferences.multipleFICible] (défaut 25× - règle des 4 %).
 * [patrimoineNetCents]     : dernier [PatrimoineSnapshot] connu (pas recalculé en direct -
 *   suffisant pour une mesure au long cours, cohérent avec la courbe déjà affichée sur
 *   l'écran Patrimoine).
 * [progressionPct]         : [patrimoineNetCents] / [capitalCibleCents] × 100, non plafonné à
 *   100 - dépasser 100 signifie que le cap est atteint.
 * [versementMensuelCents]  : versements déjà programmés, réutilisés tels quels depuis
 *   [ConseilPatrimoineResult.versementsProgrammesCents] (épargne + SCPI + abondement employeur)
 *   plutôt que ré-estimés - ce calcul existe déjà et est testé.
 * [rendementEspereAnnuelPct] : [UserPreferences.rendementFIEsperePct] - une hypothèse de
 *   simulation réglable par l'utilisateur, jamais un rendement mesuré.
 * [moisRestantsEstimes]    : null si le cap est déjà atteint, ou si les versements et le
 *   rendement choisis ne suffisent jamais à l'atteindre en deçà de 60 ans - dans ce cas
 *   l'écran doit afficher que ce n'est pas atteignable au rythme actuel, pas une durée fausse.
 */
data class CapIndependanceFinanciere(
    val currency                  : Currency,
    val depenseAnnuelleLisseeCents: Long,
    val capitalCibleCents         : Long,
    val multipleCible             : Int,
    val patrimoineNetCents        : Long,
    val progressionPct            : Int,
    val versementMensuelCents     : Long,
    val rendementEspereAnnuelPct  : Int,
    val moisRestantsEstimes       : Int?
)
