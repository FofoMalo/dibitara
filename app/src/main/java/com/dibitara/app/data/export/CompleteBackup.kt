package com.dibitara.app.data.export

import androidx.room.withTransaction
import com.dibitara.app.BuildConfig
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.repository.UserPreferencesRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first

/** Capture cohérente de toutes les tables Room ; utilisable aussi avant une restauration. */
object CompleteBackup {
    suspend fun generer(db: DibitaraDatabase, preferences: UserPreferencesRepository): String = db.withTransaction {
        val data = ExportData(
            enfants = db.childDao().getAll().first().map { it.toDomain() },
            transactions = db.transactionDao().getAll().first().map { it.toDomain() },
            budgets = db.budgetDao().getAll().first().map { it.toDomain() },
            epargne = db.savingsAccountDao().getAll().first().map { it.toDomain() },
            immobilier = db.realEstateAssetDao().getAll().first().map { it.toDomain() },
            scpi = db.scpiInvestmentDao().getAll().first().map { it.toDomain() },
            airbnb = db.airbnbRentalDao().getAll().first().map { it.toDomain() },
            vehiculeLocatif = db.vehicleRentalEntryDao().getAll().first().map { it.toDomain() },
            dettes = db.debtDao().getAll().first().map { it.toDomain() },
            actifsLibres = db.customAssetDao().getAll().first().map { it.toDomain() },
            epargneSalariale = db.employeeSavingsDao().getAll().first().map { it.toDomain() },
            sousCategoriesPerso = db.customSubCategoryDao().getAll().first().map { it.toDomain() },
            comptesBancaires = db.bankAccountDao().getAll().first().map { it.toDomain() },
            enveloppesBudget = db.categoryEnvelopeDao().getAll().first().map { it.toDomain() },
            reglesCategorisation = db.categorizationRuleDao().getAll().map { it.toDomain() },
            versementsMensuels = db.monthlyVersementDao().getAll().map { it.toDomain() },
            objectifsEpargne = db.savingsGoalDao().getAll().first().map { it.toDomain() }
        )
        val gson = GsonBuilder().setPrettyPrinting().create()
        val root = JsonParser.parseString(JsonExporter.generer(data, BuildConfig.VERSION_NAME)).asJsonObject
        root.addProperty("backupFormat", 4)
        root.add("etf_plans", gson.toJsonTree(db.etfDao().observePlans().first()))
        root.add("etf_purchases", gson.toJsonTree(db.etfDao().purchases()))
        root.add("category_definitions", gson.toJsonTree(db.categoryDefinitionDao().all()))
        root.add("patrimoine_snapshots", gson.toJsonTree(db.patrimoineSnapshotDao().getAll().first()))
        root.add("asset_valuation_snapshots", gson.toJsonTree(db.assetValuationSnapshotDao().getAll()))
        root.add("transaction_trash", gson.toJsonTree(db.transactionTrashDao().getAll().first()))
        val prefs = gson.toJsonTree(preferences.get().first()).asJsonObject
        prefs.remove("twoFactorEnabled")
        root.add("preferences", prefs)
        gson.toJson(root)
    }
}
