package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.SavingsGoal
import java.time.LocalDate

/**
 * Objectif d'épargne persisté (table `savings_goals`, Room v25).
 *
 * Pas d'index unique : l'utilisateur peut avoir plusieurs objectifs, y compris de
 * même nom. La date est stockée en jour epoch (Long), les enums en TEXT (`enum.name`),
 * relus défensivement via [safeValueOf].
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val targetDateEpochDay: Long,
    val monthlyContributionCents: Long,
    val currency: String,
    val colorKey: String,
    val iconKey: String
) {
    fun toDomain() = SavingsGoal(
        id                       = id,
        name                     = name,
        targetAmountCents        = targetAmountCents,
        currentAmountCents       = currentAmountCents,
        targetDate               = LocalDate.ofEpochDay(targetDateEpochDay),
        monthlyContributionCents = monthlyContributionCents,
        currency                 = safeValueOf(currency, Currency.EUR),
        colorKey                 = safeValueOf(colorKey, GoalColor.OR),
        iconKey                  = safeValueOf(iconKey, GoalIcon.AUTRE)
    )

    companion object {
        fun fromDomain(g: SavingsGoal) = SavingsGoalEntity(
            id                       = g.id,
            name                     = g.name,
            targetAmountCents        = g.targetAmountCents,
            currentAmountCents       = g.currentAmountCents,
            targetDateEpochDay       = g.targetDate.toEpochDay(),
            monthlyContributionCents = g.monthlyContributionCents,
            currency                 = g.currency.name,
            colorKey                 = g.colorKey.name,
            iconKey                  = g.iconKey.name
        )
    }
}
