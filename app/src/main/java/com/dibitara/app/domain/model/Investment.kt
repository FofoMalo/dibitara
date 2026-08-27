package com.dibitara.app.domain.model

import java.time.LocalDate
import kotlin.math.roundToLong

data class RealEstateAsset(
    val id: Long = 0,
    val label: String,
    val currentValueCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate,
    // Référence optionnelle vers la dette (crédit) qui finance ce bien
    val debtId: Long? = null,
    // Valeur et date à l'acquisition - saisies rétroactivement par l'utilisateur, jamais déduites
    // de l'historique de snapshots (celui-ci ne remonte pas avant la refonte du 2026-08).
    // Nullables : un bien peut ne jamais renseigner ces champs, auquel cas la carte garde
    // l'affichage "Mis à jour le" existant plutôt que le bloc d'évolution depuis acquisition.
    val acquisitionValueCents: Long? = null,
    val acquisitionDate: LocalDate? = null
)

data class ScpiInvestment(
    val id: Long = 0,
    val label: String,
    val sharesCount: Double,
    val shareValueCents: Long,
    val monthlyContributionCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate,
    val acquisitionValueCents: Long? = null,
    val acquisitionDate: LocalDate? = null
) {
    // Valeur totale = nombre de parts (peut être fractionnaire, ex : 2,2) × valeur unitaire
    val totalValueCents: Long get() = (sharesCount * shareValueCents).roundToLong()
}

data class AirbnbRental(
    val id: Long = 0,
    val propertyLabel: String,
    val amountCents: Long,
    val date: LocalDate,
    val currency: Currency
)
