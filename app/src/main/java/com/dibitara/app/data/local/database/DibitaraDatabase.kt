package com.dibitara.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.data.local.entity.AssetValuationSnapshotEntity
import com.dibitara.app.data.local.entity.BankAccountEntity
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.data.local.entity.EmployeeSavingsEntity
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import com.dibitara.app.data.local.entity.PatrimoineSnapshotEntity
import java.time.LocalDate

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
        CustomAssetEntity::class,
        EmployeeSavingsEntity::class,
        PatrimoineSnapshotEntity::class,
        CategorizationRuleEntity::class,
        CategoryEnvelopeEntity::class,
        VehicleRentalEntryEntity::class,
        AssetValuationSnapshotEntity::class,
        BankAccountEntity::class,
        SavingsGoalEntity::class
    ],
    version = 25,
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
    abstract fun customAssetDao(): CustomAssetDao
    abstract fun employeeSavingsDao(): EmployeeSavingsDao
    abstract fun patrimoineSnapshotDao(): PatrimoineSnapshotDao
    abstract fun categorizationRuleDao(): CategorizationRuleDao
    abstract fun categoryEnvelopeDao(): CategoryEnvelopeDao
    abstract fun vehicleRentalEntryDao(): VehicleRentalEntryDao
    abstract fun assetValuationSnapshotDao(): AssetValuationSnapshotDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        // Migration v24 → v25 : nouvelle table savings_goals pour les objectifs d'épargne
        // (« Voiture », « Vacances »...). Pas d'index unique : plusieurs objectifs possibles.
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetAmountCents INTEGER NOT NULL,
                        currentAmountCents INTEGER NOT NULL,
                        targetDateEpochDay INTEGER NOT NULL,
                        monthlyContributionCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        colorKey TEXT NOT NULL,
                        iconKey TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration v23 → v24 : colonne tauxAnnuelPct (REAL nullable) sur savings_accounts, pour
        // estimer les intérêts annuels (hero Épargne) et le gain annuel par compte. Colonne
        // nullable : les comptes existants ne perdent rien, l'estimation ne s'affiche que si
        // l'utilisateur renseigne le taux via le sheet.
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE savings_accounts ADD COLUMN tauxAnnuelPct REAL")
            }
        }

        // Migration v22 → v23 : valeur et date d'acquisition (saisies rétroactivement) sur les
        // 4 types d'actifs investissement, pour le bloc "Acquisition → Aujourd'hui" (écran
        // Placements). Colonnes nullables : les actifs existants ne perdent rien, le bloc ne
        // s'affiche que si l'utilisateur les renseigne via le sheet Modifier.
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE real_estate_assets ADD COLUMN acquisitionValueCents INTEGER")
                db.execSQL("ALTER TABLE real_estate_assets ADD COLUMN acquisitionDateEpochDay INTEGER")
                db.execSQL("ALTER TABLE scpi_investments ADD COLUMN acquisitionValueCents INTEGER")
                db.execSQL("ALTER TABLE scpi_investments ADD COLUMN acquisitionDateEpochDay INTEGER")
                db.execSQL("ALTER TABLE custom_assets ADD COLUMN acquisitionValueCents INTEGER")
                db.execSQL("ALTER TABLE custom_assets ADD COLUMN acquisitionDateEpochDay INTEGER")
                db.execSQL("ALTER TABLE employee_savings ADD COLUMN acquisitionValueCents INTEGER")
                db.execSQL("ALTER TABLE employee_savings ADD COLUMN acquisitionDateEpochDay INTEGER")
            }
        }

        // Migration v21 → v22 : nouvelle table bank_accounts (BRED, TradeRepublic...) + colonne
        // bankAccountId sur transactions. Seed des 2 comptes connus et backfill des transactions
        // déjà importées via leur importSource existant - aucune action requise de l'utilisateur.
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        provider TEXT NOT NULL,
                        label TEXT NOT NULL,
                        currentBalanceCents INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        updatedAtEpochDay INTEGER NOT NULL
                    )
                """.trimIndent())

                // Date fixe (pas LocalDate.now()) pour que la migration reste déterministe en test
                val dateSeed = LocalDate.of(2026, 8, 13).toEpochDay()
                db.execSQL("""
                    INSERT INTO bank_accounts (provider, label, currentBalanceCents, currency, updatedAtEpochDay)
                    VALUES ('BRED', 'BRED', 0, 'EUR', $dateSeed)
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO bank_accounts (provider, label, currentBalanceCents, currency, updatedAtEpochDay)
                    VALUES ('TRADE_REPUBLIC', 'TradeRepublic', 0, 'EUR', $dateSeed)
                """.trimIndent())

                db.execSQL("ALTER TABLE transactions ADD COLUMN bankAccountId INTEGER")

                // Rattache les transactions déjà importées à leur compte via l'importSource existant
                db.execSQL("""
                    UPDATE transactions SET bankAccountId = (SELECT id FROM bank_accounts WHERE provider = 'BRED')
                    WHERE importSource LIKE 'bred%'
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET bankAccountId = (SELECT id FROM bank_accounts WHERE provider = 'TRADE_REPUBLIC')
                    WHERE importSource = 'trade_republic'
                """.trimIndent())

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_bankAccountId ON transactions(bankAccountId)"
                )
            }
        }

        // Migration v20 → v21 : nouvelle table asset_valuation_snapshots pour les badges de tendance par actif (Immobilier/SCPI)
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS asset_valuation_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        assetType TEXT NOT NULL,
                        assetId INTEGER NOT NULL,
                        snapshotEpochDay INTEGER NOT NULL,
                        valueCents INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_asset_valuation_snapshots_asset ON asset_valuation_snapshots(assetType, assetId, snapshotEpochDay)"
                )
            }
        }

        // Migration v19 → v20 : suppression des métaux précieux (fonctionnalité retirée)
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS precious_metals")
            }
        }

        // Migration v18 → v19 : nouvelle table vehicle_rental_entries pour l'activité de location de véhicule
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vehicle_rental_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        entryType TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

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
                // La table doit correspondre EXACTEMENT à ce que Room génère pour
                // l'entité : pas de contrainte `UNIQUE(...)` en ligne mais un index
                // unique séparé `index_monthly_versements_...`. Un `UNIQUE(...)` en
                // ligne enforce la même règle mais produit une structure différente
                // dans sqlite_master, ce que `runMigrationsAndValidate` rejette
                // (base créée en v2-v5 puis migrée). Voir schemas/6.json.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_versements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        account_id INTEGER NOT NULL,
                        account_type TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        montant_cents INTEGER NOT NULL,
                        currency TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_monthly_versements_account_id_account_type_year_month` " +
                        "ON monthly_versements (`account_id`, `account_type`, `year`, `month`)"
                )
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
                // TEXT nullable, sans DEFAULT - Room accepte NULL pour les colonnes optionnelles
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
