package com.dibitara.app.domain.model

/**
 * Groupe de transactions détectées comme doublons.
 * Toutes les transactions du groupe partagent le même (date, montant, devise, type).
 *
 * [keepId] désigne la transaction à conserver — par défaut la plus ancienne (id le plus petit).
 * Les autres transactions du groupe sont candidates à la suppression.
 */
data class DuplicateGroup(
    val transactions: List<Transaction>,
    val keepId: Long = transactions.minOf { it.id }
)
