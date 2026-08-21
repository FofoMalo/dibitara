package com.dibitara.app.domain.model

/**
 * Analyse "Conseiller patrimoine" - où en sont les différentes poches d'épargne et
 * d'investissement, à partir des seules données réelles (aucune saisie utilisateur,
 * contrairement à Scénario logement).
 *
 * 4 axes indépendants, chacun solidement dérivé des données existantes - aucun ne prétend
 * juger un rendement ou une performance, que l'app ne suit pas :
 *
 * [liquiditesSuresCents]        : comptes courants (BankAccount, toutes banques) + SavingsAccount
 *                                 sûrs et liquides (Livret A, LDDS, Compte courant).
 * [objectifPrecautionCents]     : 6 mois × (besoins réels + mensualités des dettes non liées à
 *                                 un bien immobilier - le crédit d'un bien est déjà compté via
 *                                 la dépense réelle catégorisée LOGEMENT, même logique que
 *                                 Scénario logement pour éviter le double-compte).
 * [precautionSuffisante]        : liquiditésSûres >= objectifPrécaution.
 *
 * [pochesAvecMarge]             : comptes avec un plafond renseigné et du solde restant avant
 *                                 de l'atteindre - purement informatif, sans ordre de priorité
 *                                 imposé (l'app ne connaît pas l'horizon/les objectifs personnels).
 *
 * [objectifEpargneMensuelCents] : objectif 20% du revenu moyen (UserPreferences.tauxEpargneCiblePct).
 * [resteAVivreReelCents]        : revenu moyen - besoins réels - autres dettes (ce qui est
 *                                 réellement disponible chaque mois, cf. [objectifPlafonneParResteAVivre]).
 * [versementsProgrammesCents]   : somme des versements mensuels déjà programmés (épargne + SCPI).
 * [capaciteNonAffecteeCents]    : capacité réaliste (min de l'objectif 20% et du reste à vivre
 *                                 réel) non encore dirigée vers une poche.
 * [objectifPlafonneParResteAVivre] : true si le reste à vivre réel est inférieur à l'objectif
 *                                 20% - la capacité affichée est alors plafonnée par ce qui existe
 *                                 réellement, pas un chiffre aspirationnel.
 *
 * [repartitionParCategorie]     : poids de chaque catégorie dans le patrimoine net investi
 *                                 (immobilier net du crédit lié à chaque bien, SCPI, actifs
 *                                 libres, épargne salariale, épargne - même périmètre que
 *                                 PatrimonyOverview.patrimoineBrutCents, immobilier en net).
 * [categorieSurConcentree]      : la catégorie dépassant 70% du total, null si aucune.
 */
data class ConseilPatrimoineResult(
    val currency                       : Currency,
    val liquiditesSuresCents           : Long,
    val objectifPrecautionCents        : Long,
    val precautionSuffisante           : Boolean,
    val pochesAvecMarge                : List<PocheAvecMarge>,
    val objectifEpargneMensuelCents    : Long,
    val resteAVivreReelCents           : Long,
    val versementsProgrammesCents      : Long,
    val capaciteNonAffecteeCents       : Long,
    val objectifPlafonneParResteAVivre : Boolean,
    val repartitionParCategorie        : List<RepartitionCategorie>,
    val categorieSurConcentree         : RepartitionCategorie?
)

data class PocheAvecMarge(
    val label        : String,
    val soldeCents    : Long,
    val plafondCents  : Long,
    val margeCents    : Long
)

enum class CategoriePatrimoine(val displayName: String) {
    EPARGNE          ("Épargne"),
    IMMOBILIER       ("Immobilier"),
    SCPI             ("SCPI"),
    ACTIFS_LIBRES    ("Actifs libres"),
    EPARGNE_SALARIALE("Épargne salariale")
}

data class RepartitionCategorie(
    val categorie      : CategoriePatrimoine,
    val montantCents    : Long,
    val pourcentage      : Float
)

/** Seuil au-delà duquel une catégorie est signalée comme sur-concentrée dans le patrimoine. */
const val SEUIL_CONCENTRATION_PCT = 70f
