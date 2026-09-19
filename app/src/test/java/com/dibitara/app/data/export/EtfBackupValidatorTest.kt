package com.dibitara.app.data.export

import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EtfBackupValidatorTest {
    private val date = LocalDate.now()
    private val asset = CustomAsset(1, "CTO", 123_000, Currency.EUR, date)
    private val plan = EtfPlanEntity.fromDomain(EtfPlan(1, "ETF", 2, 100_000, date.minusDays(1), 5_000, date, Currency.EUR))
    private val purchase = EtfPurchaseEntity.fromDomain(EtfPurchase(8, 1, date, 19_900, 100, 4))

    @Test fun `serialization conserve references frais et liens`() {
        val gson = Gson()
        val p = gson.fromJson(gson.toJson(plan), EtfPlanEntity::class.java)
        val b = gson.fromJson(gson.toJson(purchase), EtfPurchaseEntity::class.java)
        assertEquals(plan, p)
        assertEquals(purchase, b)
        EtfBackupValidator.validate(listOf(p), listOf(b), listOf(asset))
    }
    @Test fun `ancien format sans suivi ETF reste valide`() {
        EtfBackupValidator.validate(emptyList(), emptyList(), listOf(asset))
    }
    @Test fun `restauration refuse doublons et liens orphelins avant ecriture`() {
        val invalid = listOf(
            listOf(purchase, purchase.copy(id = 9)),
            listOf(purchase.copy(assetId = 99)),
            listOf(purchase.copy(dateEpochDay = plan.referenceEpochDay)),
            listOf(purchase.copy(amountCents = -1)),
            listOf(purchase.copy(id = 0)))
        invalid.forEach { rows -> assertThrows(IllegalArgumentException::class.java) { EtfBackupValidator.validate(listOf(plan), rows, listOf(asset)) } }
        assertThrows(IllegalArgumentException::class.java) { EtfBackupValidator.validate(listOf(plan, plan), emptyList(), listOf(asset)) }
        assertThrows(IllegalArgumentException::class.java) { EtfBackupValidator.validate(listOf(plan), emptyList(), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { EtfBackupValidator.validate(listOf(plan), emptyList(), listOf(asset.copy(currency = Currency.USD))) }
    }
    @Test fun `format quatre exige les deux collections ETF`() {
        val root = JsonObject().apply {
            addProperty("version", "4.14.0"); addProperty("exportDate", date.toString()); addProperty("backupFormat", 4)
            listOf("enfants", "transactions", "budgets", "epargne", "immobilier", "scpi", "airbnb", "vehicule_locatif", "dettes",
                "actifs_libres", "epargne_salariale", "sous_categories_perso", "comptes_bancaires", "enveloppes_budget", "regles_categorisation",
                "versements_mensuels", "objectifs_epargne", "patrimoine_snapshots", "asset_valuation_snapshots", "transaction_trash", "category_definitions",
                "etf_plans", "etf_purchases").forEach { add(it, JsonArray()) }
            add("preferences", JsonObject())
        }
        assertEquals(0, BackupJsonVerifier.verifier(root.toString()))
        root.remove("etf_purchases")
        assertThrows(IllegalArgumentException::class.java) { BackupJsonVerifier.verifier(root.toString()) }
        root.addProperty("backupFormat", 3)
        assertEquals(0, BackupJsonVerifier.verifier(root.toString()))
    }
}
