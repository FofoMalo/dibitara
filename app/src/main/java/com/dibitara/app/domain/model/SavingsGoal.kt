package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Un objectif d'épargne nommé (« Voiture », « Vacances », « Apport »...) - le cœur
 * « orienté objectifs » de l'écran Épargne (refonte §8).
 *
 * [currentAmountCents] est SAISI MANUELLEMENT par défaut ([FundingMode.MANUEL]) : il
 * n'est pas déduit des [SavingsAccount]. Un objectif est un projet, pas un miroir d'un
 * compte - on peut viser 15 000 € pour une voiture en piochant sur plusieurs livrets à
 * la fois. Voir [FundingMode] pour les deux modes où ce n'est plus vrai.
 *
 * [colorKey] et [iconKey] sont des CLÉS stables (enum), jamais un index positionnel :
 * la couleur d'un objectif ne doit pas changer selon sa place dans la liste (même
 * règle que [com.dibitara.app.presentation.common.chartColor]).
 *
 * [sourceAccountId] et [fundingMode] sont NULLABLES côté domaine (pas seulement en base)
 * bien qu'un objectif ait toujours un mode effectif ([fundingModeEffectif]) : la
 * sauvegarde/restauration JSON désérialise ces objets par réflexion (Gson), qui
 * n'appelle pas le constructeur Kotlin et ignore donc toute valeur par défaut - un
 * ancien fichier de sauvegarde sans ces deux clés donnerait `null` de toute façon, pas
 * `FundingMode.MANUEL`. Les garder nullables rend cet état explicite plutôt que de
 * laisser un `enum` non-null mentir après une restauration ancienne.
 */
data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val targetDate: LocalDate,
    /** 0 = pas de plan de versement mensuel → pas de projection de date, juste le %. */
    val monthlyContributionCents: Long,
    val currency: Currency,
    val colorKey: GoalColor,
    val iconKey: GoalIcon,
    /** Compte épargne lié - n'a d'effet que si [fundingModeEffectif] == [FundingMode.SOLDE_COMPTE]. */
    val sourceAccountId: Long? = null,
    val fundingMode: FundingMode? = null
) {
    /**
     * Avancement 0f..1f, borné (un dépassement s'affiche « atteint », pas > 100 %).
     * La saisie garantit `targetAmountCents > 0` ; le `<= 0` ici n'est qu'un garde-fou
     * pour ne jamais diviser par zéro.
     *
     * Lit `currentAmountCents` tel quel : pour un objectif en [FundingMode.SOLDE_COMPTE],
     * c'est à l'appelant (voir `ResoudreMontantObjectifUseCase`) de fournir un
     * `SavingsGoal` dont `currentAmountCents` porte déjà le solde du compte lié converti -
     * ce type ne fait aucun accès compte/devise lui-même (couche domaine pure).
     */
    val progression: Float
        get() = if (targetAmountCents <= 0) 0f
                else (currentAmountCents.toFloat() / targetAmountCents).coerceIn(0f, 1f)

    /** L'objectif est financé (montant épargné ≥ montant visé). */
    val estAtteint: Boolean
        get() = targetAmountCents > 0 && currentAmountCents >= targetAmountCents

    /** [fundingMode] avec son défaut appliqué - voir la note sur la nullabilité ci-dessus. */
    val fundingModeEffectif: FundingMode
        get() = fundingMode ?: FundingMode.MANUEL
}

/** Palette d'accent d'un objectif - 3 teintes de la data-viz (or / teal / mauve). */
enum class GoalColor { OR, TEAL, MAUVE }

/** Jeu fixe d'icônes proposées à l'utilisateur pour illustrer un objectif. */
enum class GoalIcon { VOYAGE, VOITURE, MAISON, ETUDES, CADEAU, PRECAUTION, AUTRE }

/**
 * Comment `SavingsGoal.currentAmountCents` est alimenté (cadrage
 * CADRAGE_OBJECTIFS_CONNECTES.md §4) :
 * - [MANUEL] : comportement historique, saisi à la main dans la feuille d'édition.
 * - [VERSEMENTS] : même stockage que MANUEL (le champ reste un simple seed initial),
 *   mais avancé par le bouton « Verser » existant (`AppliquerVersementsObjectifUseCase`,
 *   versements `CompteType.OBJECTIF`) plutôt que retapé à la main chaque mois.
 * - [SOLDE_COMPTE] : dérivé en LECTURE SEULE du solde de [SavingsGoal.sourceAccountId] -
 *   la valeur stockée n'est jamais lue tant que ce mode est actif (voir
 *   `ResoudreMontantObjectifUseCase`) ; elle ne redevient significative qu'après une
 *   déliaison (retour à MANUEL), où elle est figée à la dernière valeur affichée.
 */
enum class FundingMode { MANUEL, VERSEMENTS, SOLDE_COMPTE }
