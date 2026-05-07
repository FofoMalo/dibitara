package com.dibitara.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.entity.*

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
        AirbnbRentalEntity::class
    ],
    version = 2,
    exportSchema = false
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

    companion object {
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
