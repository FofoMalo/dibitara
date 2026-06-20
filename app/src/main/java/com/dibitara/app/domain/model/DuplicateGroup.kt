package com.dibitara.app.domain.model

/**
 * Groupe de transactions détectées comme doublons.
 * Toutes les transactions du groupe partagent le même (date, montant, devise, type).
 *
 * [keepIds] : ensemble des identifiants à conserver. Par défaut, seule la plus ancienne
 * (id le plus petit) est conservée et les autres sont candidates à la suppression.
 *
 * L'utilisateur peut cocher plusieurs transactions pour les conserver toutes - ce qui
 * est utile quand deux achats identiques le même jour sont de vraies transactions distinctes
 * et non de vrais doublons (faux positifs).
 */
data class DuplicateGroup(
    val transactions: List<Transaction>,
    val keepIds: Set<Long> = setOf(transactions.minOf { it.id })
)
