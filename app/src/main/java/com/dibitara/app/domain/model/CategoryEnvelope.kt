package com.dibitara.app.domain.model

/**
 * Enveloppe budgétaire mensuelle par catégorie.
 *
 * Un plafond permanent — pas lié à un mois spécifique.
 * Le "dépensé" est calculé dynamiquement depuis les transactions du mois affiché.
 */
data class CategoryEnvelope(
    val id           : Long     = 0,
    val category     : Category,
    val plafondCents : Long,
    val currency     : Currency
)

/**
 * Statut calculé d'une enveloppe pour un mois donné.
 * N'est jamais stocké en base — recalculé à chaque observation du Flow.
 *
 * @param taux depenseCents / plafondCents — peut dépasser 1.0 si le plafond est atteint.
 */
data class EnveloppeStatus(
    val envelope     : CategoryEnvelope,
    val depenseCents : Long,
    val taux         : Float
) {
    /** Alerte orange : entre 80 % et 100 % du plafond. */
    val isAlerte  : Boolean get() = taux >= 0.8f && !isDepasse

    /** Alerte rouge : plafond dépassé. */
    val isDepasse : Boolean get() = taux >= 1.0f

    /** Centimes restants avant d'atteindre le plafond (0 si déjà dépassé). */
    val restantCents : Long get() = (envelope.plafondCents - depenseCents).coerceAtLeast(0L)
}
