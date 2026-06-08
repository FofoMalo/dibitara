package com.dibitara.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.data.local.entity.EmployeeSavingsEntity
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import com.dibitara.app.data.local.entity.PatrimoineSnapshotEntity

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
        MonthlyVersementEntity::class,
        PreciousMetalEntity::class,
        CustomAssetEntity::class,
        EmployeeSavingsEntity::class,
        PatrimoineSnapshotEntity::class,
        CategorizationRuleEntity::class,
        CategoryEnvelopeEntity::class
    ],
    version = 18,
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
    abstract fun preciousMetalDao(): PreciousMetalDao
    abstract fun customAssetDao(): CustomAssetDao
    abstract fun employeeSavingsDao(): EmployeeSavingsDao
    abstract fun patrimoineSnapshotDao(): PatrimoineSnapshotDao
    abstract fun categorizationRuleDao(): CategorizationRuleDao
    abstract fun categoryEnvelopeDao(): CategoryEnvelopeDao

    companion object {
        // Migration v17 → v18 : nouvelle table category_envelopes pour les enveloppes budgétaires par catégorie
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS category_envelopes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        plafondCents INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_category_envelopes_category ON category_envelopes(category)"
                )
            }
        }

        // Migration v16 → v17 : nouvelle table categorization_rules pour l'apprentissage des catégorisations manuelles
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categorization_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noteExact TEXT NOT NULL,
                        category TEXT NOT NULL,
                        subCategory TEXT,
                        customSubCategoryId INTEGER
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_categorization_rules_noteExact ON categorization_rules(noteExact)"
                )
            }
        }

        // Migration v15 → v16 : taux d'intérêt annuel sur debts (branche florent/prive)
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN tauxInteret REAL")
            }
        }

        // Migration v14 → v15 : colonnes paymentDay et originalAmountCents sur debts
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN paymentDay INTEGER")
                db.execSQL("ALTER TABLE debts ADD COLUMN originalAmountCents INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration v13 → v14 : colonne plafondCents sur savings_accounts pour le suivi du plafond légal
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE savings_accounts ADD COLUMN plafondCents INTEGER")
            }
        }

        // Migration v12 → v13 : nouvelle table patrimoine_snapshots pour l'historique mensuel
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS patrimoine_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        snapshotEpochDay INTEGER NOT NULL,
                        patrimoineBrutCents INTEGER NOT NULL,
                        patrimoineNetCents INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration v11 → v12 : colonne debtId sur real_estate_assets pour lier un crédit à un bien
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE real_estate_assets ADD COLUMN debtId INTEGER")
            }
        }

        // Migration v10 → v11 : 2 colonnes sur transactions pour l'import TradeRepublic
        // importSource identifie l'origine ("trade_republic"), externalId est l'UUID TR pour la déduplication
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN importSource TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN externalId TEXT")
            }
        }

        // Migration v9 → v10 : 3 nouvelles tables pour les investissements personnalisés
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS precious_metals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        metalType TEXT NOT NULL,
                        label TEXT NOT NULL,
                        quantityGrams REAL NOT NULL,
                        pricePerGramCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        totalValueCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS employee_savings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        label TEXT NOT NULL,
                        currentBalanceCents INTEGER NOT NULL,
                        employerContributionCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration v8 → v9 : sharesCount passe de INTEGER à REAL pour les parts fractionnées (ex : 2,2 parts)
        // SQLite n'accepte pas ALTER TABLE MODIFY COLUMN → on recrée la table entièrement
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scpi_investments_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        sharesCount REAL NOT NULL,
                        shareValueCents INTEGER NOT NULL,
                        monthlyContributionCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())
                // Les anciennes valeurs entières (ex : 2) sont copiées comme REAL (2.0) sans perte
                db.execSQL("INSERT INTO scpi_investments_new SELECT * FROM scpi_investments")
                db.execSQL("DROP TABLE scpi_investments")
                db.execSQL("ALTER TABLE scpi_investments_new RENAME TO scpi_investments")
            }
        }

        // Migration v7 → v8 : récurrences enrichies (fréquence, firstPaymentDate, endDate)
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceFrequency TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN firstPaymentDateEpochDay INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN endDateEpochDay INTEGER")
                // Les modèles mensuels existants basculent vers MONTHLY + firstPaymentDate = date du modèle
                db.execSQL("UPDATE transactions SET recurrenceFrequency = 'MONTHLY', firstPaymentDateEpochDay = dateEpochDay WHERE isRecurring = 1")
            }
        }

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
