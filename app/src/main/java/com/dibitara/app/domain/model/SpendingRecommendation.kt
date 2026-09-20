package com.dibitara.app.domain.model

/**
 * Classification 50/30/20 des catégories de dépenses.
 *
 * Règle financière standard : 50 % des revenus pour les besoins incompressibles,
 * 30 % pour les envies, 20 % pour l'épargne et le remboursement des dettes.
 * On l'affiche comme boussole, pas comme contrainte rigide.
 *
 * [pourcentageCible] sert à calculer combien de centimes allouer théoriquement
 * à chaque famille à partir du revenu moyen.
 */
enum class BudgetBucket(val displayName: String, val pourcentageCible: Int) {
    BESOINS          ("Besoins essentiels", 50),
    ENVIES           ("Envies & loisirs",   30),
    EPARGNE_ET_DETTES("Épargne & dettes",   20)
}

/**
 * Mapping de chaque catégorie vers son bucket 50/30/20.
 * INVESTISSEMENT et EPARGNE font partie du bucket EPARGNE_ET_DETTES.
 * AUTRE et TRANSFERTS sont exclus (trop génériques pour être recommandés).
 */
val Category.bucket: BudgetBucket?
    get() = when (this) {
        Category.ALIMENTATION,
        Category.LOGEMENT,
        Category.TRANSPORT,
        Category.SANTE,
        Category.IMPOTS_CHARGES,
        Category.ASSURANCES,
        Category.EDUCATION,
        Category.ENFANT         -> BudgetBucket.BESOINS

        Category.LOISIRS,
        Category.ABONNEMENTS,
        Category.HABILLEMENT,
        Category.TABAC,
        Category.TRANSFERTS_FAMILIAUX -> BudgetBucket.ENVIES

        Category.EPARGNE,
        Category.INVESTISSEMENT  -> BudgetBucket.EPARGNE_ET_DETTES

        // AUTRE et TRANSFERTS exclus volontairement - trop vagues pour une recommandation fiable
        Category.AUTRE,
        Category.TRANSFERTS      -> null
        // Une catégorie personnelle n’est pas supposée être un besoin ou une envie.
        else -> null
    }

/**
 * Recommandation de plafond pour une catégorie de dépense.
 *
 * [moyenneCents]       : dépense moyenne réelle sur les 3 derniers mois.
 * [recommandeCents]    : plafond suggéré = moyenne arrondie au 10 € supérieur.
 *                        Légèrement au-dessus de la moyenne pour éviter d'être trop contraignant.
 * [bucket]             : famille 50/30/20 de cette catégorie.
 * [enveloppeExistante] : enveloppe déjà configurée (Sprint 40), null si aucune.
 * [concentrationPct]   : part du total du trimestre portée par les 1-2 plus grosses
 *                        transactions de la catégorie (0-100). Une catégorie mal
 *                        catégorisée (ex. un frais récurrent mal classé) produit le même
 *                        profil temporel qu'une vraie dérive de dépense - la concentration
 *                        les distingue avant toute classification signal/bruit
 *                        (CADRAGE_INDEPENDANCE_FINANCIERE.md §1/§6, cas réel constaté :
 *                        un frais bancaire classé Transport).
 * [moisAvecDepense]    : combien des 3 mois analysés ont au moins une dépense dans cette
 *                        catégorie (1 à 3). Base de [natureTemporelle] - F4 du cadrage.
 */
data class PocheRecommandee(
    val category           : Category,
    val moyenneCents       : Long,
    val recommandeCents    : Long,
    val bucket             : BudgetBucket,
    val enveloppeExistante : CategoryEnvelope? = null,
    val concentrationPct   : Int = 0,
    val moisAvecDepense    : Int = 3
) {
    /**
     * Écart entre la recommandation et l'enveloppe existante.
     * Positif = recommandation plus généreuse, négatif = recommandation plus stricte.
     * Null si aucune enveloppe n'est configurée.
     */
    val ecartAvecEnveloppe: Long?
        get() = enveloppeExistante?.let { recommandeCents - it.plafondCents }

    /**
     * true si [concentrationPct] atteint le seuil - à afficher comme « à vérifier »
     * plutôt que comme une dépense fiable. Seuil par défaut prudent (cas réel observé :
     * ~100%) ; à recalibrer sur davantage d'historique réel, pas figé par construction.
     */
    val aVerifier: Boolean
        get() = concentrationPct >= SEUIL_CONCENTRATION_PCT

    /**
     * Signal/bruit (F4) : SIGNAL si la catégorie a une dépense chaque mois analysé (dérive
     * durable), BRUIT si elle n'apparaît que sur un seul mois (accident ponctuel),
     * INDETERMINE sinon (2 mois sur 3 - pas assez tranché pour se prononcer).
     *
     * Se lit seulement si [aVerifier] est faux : une catégorie dominée par 1-2 grosses
     * transactions (ex. le cas réel Transport/frais de gestion) peut produire un faux
     * SIGNAL ou un faux BRUIT selon la répartition des mois - la concentration doit être
     * résolue en premier (CADRAGE_INDEPENDANCE_FINANCIERE.md §6).
     */
    val natureTemporelle: NatureTemporelle
        get() = when (moisAvecDepense) {
            3    -> NatureTemporelle.SIGNAL
            1    -> NatureTemporelle.BRUIT
            else -> NatureTemporelle.INDETERMINE
        }

    companion object {
        const val SEUIL_CONCENTRATION_PCT = 70
    }
}

enum class NatureTemporelle { SIGNAL, BRUIT, INDETERMINE }

/**
 * Recommandation financière mensuelle complète, calculée à chaque affichage.
 * Aucune donnée n'est persistée en base - ce modèle est recalculé à partir des 3 derniers mois.
 *
 * [revenuMoyenCents]         : moyenne des revenus sur les 3 mois analysés.
 * [revenuParMoisCents]       : revenu de chacun des 3 mois analysés, du plus récent au plus
 *                              ancien (même ordre que [moisDeReference]). Sert à détecter un
 *                              mois anormalement bas par rapport à la moyenne (voir
 *                              [com.dibitara.app.domain.usecase.CheckRevenuIncompletUseCase]).
 * [depensesMoyennesCents]    : moyenne des dépenses (type EXPENSE, virements internes exclus,
 *                              toutes catégories confondues) sur les 3 mois analysés. Sert de
 *                              base à [tauxEpargneActuelPct] et au capital cible d'indépendance
 *                              financière (voir [com.dibitara.app.domain.usecase.GetCapIndependanceFinanciereUseCase]).
 * [engagementsMensuels]      : somme incompressible = mensualités dettes + contributions épargne.
 * [tauxEpargneActuelPct]     : taux d'épargne moyen réel observé (null si aucun revenu).
 * [tauxEpargneCiblePct]      : objectif configuré dans les préférences.
 * [objectifEpargneCents]     : revenuMoyen × tauxEpargneCible / 100.
 * [pouchesRecommandees]      : une entrée par catégorie avec au moins une dépense sur 3 mois.
 * [soldePrevisionelCents]    : revenuMoyen - engagements - sum(poches) - doit être ≥ 0.
 * [estEquilibre]             : true si le plan est viable sans déficit.
 * [moisDeReference]          : les 3 mois (mois, année) analysés, du plus récent au plus ancien.
 */
data class SpendingRecommendation(
    val currency               : Currency,
    val revenuMoyenCents       : Long,
    val revenuParMoisCents     : List<Long>,
    val depensesMoyennesCents  : Long,
    val engagementsMensuels    : Long,
    val tauxEpargneActuelPct   : Int?,
    val tauxEpargneCiblePct    : Int,
    val objectifEpargneCents   : Long,
    val pouchesRecommandees    : List<PocheRecommandee>,
    val soldePrevisionelCents  : Long,
    val estEquilibre           : Boolean,
    val moisDeReference        : List<Pair<Int, Int>>
)
