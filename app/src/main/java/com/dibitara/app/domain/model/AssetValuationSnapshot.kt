package com.dibitara.app.domain.model

import java.time.LocalDate

enum class AssetValuationType { REAL_ESTATE, SCPI }

/**
 * Instantané de la valeur d'un actif (bien immobilier ou SCPI) à une date donnée.
 * Capturé automatiquement à chaque ajout/modification de la valeur de l'actif -
 * jamais sur une planification, jamais via une saisie manuelle dédiée.
 */
data class AssetValuationSnapshot(
    val id: Long = 0,
    val assetType: AssetValuationType,
    val assetId: Long,
    val snapshotDate: LocalDate,
    val valueCents: Long,
    val currency: Currency
)
