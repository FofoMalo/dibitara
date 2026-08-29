package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Un objectif d'épargne nommé (« Voiture », « Vacances », « Apport »...) - le cœur
 * « orienté objectifs » de l'écran Épargne (refonte §8).
 *
 * [currentAmountCents] est SAISI MANUELLEMENT par l'utilisateur : il n'est pas déduit
 * des [SavingsAccount]. Un objectif est un projet, pas un miroir d'un compte - on peut
 * viser 15 000 € pour une voiture en piochant sur plusieurs livrets à la fois.
 *
 * [colorKey] et [iconKey] sont des CLÉS stables (enum), jamais un index positionnel :
 * la couleur d'un objectif ne doit pas changer selon sa place dans la liste (même
 * règle que [com.dibitara.app.presentation.common.chartColor]).
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
    val iconKey: GoalIcon
) {
    /**
     * Avancement 0f..1f, borné (un dépassement s'affiche « atteint », pas > 100 %).
     * La saisie garantit `targetAmountCents > 0` ; le `<= 0` ici n'est qu'un garde-fou
     * pour ne jamais diviser par zéro.
     */
    val progression: Float
        get() = if (targetAmountCents <= 0) 0f
                else (currentAmountCents.toFloat() / targetAmountCents).coerceIn(0f, 1f)

    /** L'objectif est financé (montant épargné ≥ montant visé). */
    val estAtteint: Boolean
        get() = targetAmountCents > 0 && currentAmountCents >= targetAmountCents
}

/** Palette d'accent d'un objectif - 3 teintes de la data-viz (or / teal / mauve). */
enum class GoalColor { OR, TEAL, MAUVE }

/** Jeu fixe d'icônes proposées à l'utilisateur pour illustrer un objectif. */
enum class GoalIcon { VOYAGE, VOITURE, MAISON, ETUDES, CADEAU, PRECAUTION, AUTRE }
