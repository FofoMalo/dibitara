package com.dibitara.app.data.local.entity

import androidx.room.*
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.EtfPlan
import com.dibitara.app.domain.model.EtfPurchase
import java.time.LocalDate

@Entity(tableName = "etf_plans", foreignKeys = [ForeignKey(
    entity = CustomAssetEntity::class, parentColumns = ["id"], childColumns = ["assetId"], onDelete = ForeignKey.CASCADE
)])
data class EtfPlanEntity(
    @PrimaryKey val assetId: Long,
    val name: String,
    val sourceAccountId: Long,
    val initialInvestedCents: Long,
    val referenceEpochDay: Long,
    val weeklyAmountCents: Long,
    val nextPurchaseEpochDay: Long,
    val currency: String
) {
    fun toDomain() = EtfPlan(assetId, name, sourceAccountId, initialInvestedCents,
        LocalDate.ofEpochDay(referenceEpochDay), weeklyAmountCents, LocalDate.ofEpochDay(nextPurchaseEpochDay), Currency.valueOf(currency))
    companion object {
        fun fromDomain(p: EtfPlan) = EtfPlanEntity(p.assetId, p.name, p.sourceAccountId, p.initialInvestedCents,
            p.referenceDate.toEpochDay(), p.weeklyAmountCents, p.nextPurchaseDate.toEpochDay(), p.currency.name)
    }
}

// Pas de FK vers transactions : mettre l'opération à la corbeille ne doit pas effacer un achat validé.
// L'identifiant reste réservé jusqu'à la suppression explicite de l'achat dans le suivi ETF.
@Entity(tableName = "etf_purchases", foreignKeys = [ForeignKey(
    entity = EtfPlanEntity::class, parentColumns = ["assetId"], childColumns = ["assetId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("assetId"), Index(value = ["transactionId"], unique = true)])
data class EtfPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val dateEpochDay: Long,
    val amountCents: Long,
    val feesCents: Long,
    val transactionId: Long?
) {
    fun toDomain() = EtfPurchase(id, assetId, LocalDate.ofEpochDay(dateEpochDay), amountCents, feesCents, transactionId)
    companion object {
        fun fromDomain(p: EtfPurchase) = EtfPurchaseEntity(p.id, p.assetId, p.date.toEpochDay(), p.amountCents, p.feesCents, p.transactionId)
    }
}
