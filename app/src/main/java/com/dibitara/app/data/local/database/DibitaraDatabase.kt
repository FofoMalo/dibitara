package com.dibitara.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.data.local.entity.MonthlyVersementEntity

/**
 * Base de données Room locale.
 *
 * RÈGLE IMPORTANTE : chaque modification du schéma incrémente [version]
 * ET requiert une migration écrite dans [MIGRATION_1_2], etc.
 * Ne jamais utiliser fallbackToDestructiveMigration en production.
 */
@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        ChildEntity::class,
        DebtEntity::class,
        SavingsAccountEntity::class,
        RealEstateAssetEntity::class,
        ScpiInvestmentEntity::class,
        AirbnbRentalEntity::class,
        CustomSubCategoryEntity::class,
        MonthlyVersementEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class DibitaraDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun childDao(): ChildDao
    abstract fun debtDao(): DebtDao
    abstract fun savingsAccountDao(): SavingsAccountDao
    abstract fun realEstateAssetDao(): RealEstateAssetDao
    abstract fun scpiInvestmentDao(): ScpiInvestmentDao
    abstract fun airbnbRentalDao(): AirbnbRentalDao
    abstract fun customSubCategoryDao(): CustomSubCategoryDao
    abstract fun monthlyVersementDao(): MonthlyVersementDao

    companion object {
        // Migration v6 → v7 : restructuration des catégories
        // VACANCES fusionnée dans LOISIRS ; TELEPHONIE et INFORMATIQUE promues en catégorie ABONNEMENTS
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET category = 'LOISIRS' WHERE category = 'VACANCES'")
                db.execSQL("UPDATE transactions SET category = 'ABONNEMENTS', subCategory = NULL WHERE subCategory = 'TELEPHONIE'")
                db.execSQL("UPDATE transactions SET category = 'ABONNEMENTS', subCategory = NULL WHERE subCategory = 'INFORMATIQUE'")
                // Les sous-catégories personnalisées rattachées à VACANCES basculent vers LOISIRS
                db.execSQL("UPDATE custom_sub_categories SET parentCategory = 'LOISIRS' WHERE parentCategory = 'VACANCES'")
            }
        }

        // Migration v5 → v6 : nouvelle table monthly_versements pour les versements mensuels
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_versements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        account_id INTEGER NOT NULL,
                        account_type TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        montant_cents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        UNIQUE(account_id, account_type, year, month)
                    )
                """.trimIndent())
            }
        }

        // Migration v4 → v5 : nouvelle table custom_sub_categories + colonne customSubCategoryId
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_sub_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        parentCategory TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE transactions ADD COLUMN customSubCategoryId INTEGER")
            }
        }

        // Migration v3 → v4 : ajout de la sous-catégorie pour AUTRE
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // TEXT nullable, sans DEFAULT — Room accepte NULL pour les colonnes optionnelles
                db.execSQL("ALTER TABLE transactions ADD COLUMN subCategory TEXT")
            }
        }

        // Migration v2 → v3 : ajout des champs pour les transactions récurrentes
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // isRecurring stocké comme INTEGER (0/1) car Room ne supporte pas BOOLEAN en SQL
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceDay INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceRecurringId INTEGER")
            }
        }

        // Migration v1 → v2 : ajout du champ childId sur transactions + nouvelles tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN childId INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS children (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        totalCents INTEGER NOT NULL,
                        monthlyPaymentCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        type TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        label TEXT NOT NULL,
                        currentBalanceCents INTEGER NOT NULL,
                        monthlyContributionCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        childId INTEGER,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS real_estate_assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        currentValueCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scpi_investments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        sharesCount INTEGER NOT NULL,
                        shareValueCents INTEGER NOT NULL,
                        monthlyContributionCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS airbnb_rentals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        propertyLabel TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
