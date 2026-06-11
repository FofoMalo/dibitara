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
        Category.TRANSFERTS_FAMILIAUX -> BudgetBucket.ENVIES

        Category.EPARGNE,
        Category.INVESTISSEMENT  -> BudgetBucket.EPARGNE_ET_DETTES

        // AUTRE et TRANSFERTS exclus volontairement — trop vagues pour une recommandation fiable
        Category.AUTRE,
        Category.TRANSFERTS      -> null
    }

/**
 * Recommandation de plafond pour une catégorie de dépense.
 *
 * [moyenneCents]       : dépense moyenne réelle sur les 3 derniers mois.
 * [recommandeCents]    : plafond suggéré = moyenne arrondie au 10 € supérieur.
 *                        Légèrement au-dessus de la moyenne pour éviter d'être trop contraignant.
 * [bucket]             : famille 50/30/20 de cette catégorie.
 * [enveloppeExistante] : enveloppe déjà configurée (Sprint 40), null si aucune.
 */
data class PocheRecommandee(
    val category           : Category,
    val moyenneCents       : Long,
    val recommandeCents    : Long,
    val bucket             : BudgetBucket,
    val enveloppeExistante : CategoryEnvelope? = null
) {
    /**
     * Écart entre la recommandation et l'enveloppe existante.
     * Positif = recommandation plus généreuse, négatif = recommandation plus stricte.
     * Null si aucune enveloppe n'est configurée.
     */
    val ecartAvecEnveloppe: Long?
        get() = enveloppeExistante?.let { recommandeCents - it.plafondCents }
}

/**
 * Recommandation financière mensuelle complète, calculée à chaque affichage.
 * Aucune donnée n'est persistée en base — ce modèle est recalculé à partir des 3 derniers mois.
 *
 * [revenuMoyenCents]         : moyenne des revenus sur les 3 mois analysés.
 * [engagementsMensuels]      : somme incompressible = mensualités dettes + contributions épargne.
 * [tauxEpargneActuelPct]     : taux d'épargne moyen réel observé (null si aucun revenu).
 * [tauxEpargneCiblePct]      : objectif configuré dans les préférences.
 * [objectifEpargneCents]     : revenuMoyen × tauxEpargneCible / 100.
 * [pouchesRecommandees]      : une entrée par catégorie avec au moins une dépense sur 3 mois.
 * [soldePrevisionelCents]    : revenuMoyen - engagements - sum(poches) — doit être ≥ 0.
 * [estEquilibre]             : true si le plan est viable sans déficit.
 * [moisDeReference]          : les 3 mois (mois, année) analysés, du plus récent au plus ancien.
 */
data class SpendingRecommendation(
    val currency               : Currency,
    val revenuMoyenCents       : Long,
    val engagementsMensuels    : Long,
    val tauxEpargneActuelPct   : Int?,
    val tauxEpargneCiblePct    : Int,
    val objectifEpargneCents   : Long,
    val pouchesRecommandees    : List<PocheRecommandee>,
    val soldePrevisionelCents  : Long,
    val estEquilibre           : Boolean,
    val moisDeReference        : List<Pair<Int, Int>>
)
