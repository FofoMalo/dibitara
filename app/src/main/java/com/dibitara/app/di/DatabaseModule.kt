package com.dibitara.app.di

import android.content.Context
import androidx.room.Room
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.dao.CategorizationRuleDao
import com.dibitara.app.data.local.dao.CategoryEnvelopeDao
import com.dibitara.app.data.local.dao.CustomAssetDao
import com.dibitara.app.data.local.dao.EmployeeSavingsDao
import com.dibitara.app.data.local.dao.MonthlyVersementDao
import com.dibitara.app.data.local.dao.PatrimoineSnapshotDao
import com.dibitara.app.data.local.dao.PreciousMetalDao
import com.dibitara.app.data.repository.*
import com.dibitara.app.data.repository.CategorizationRuleRepositoryImpl
import com.dibitara.app.data.repository.CategoryEnvelopeRepositoryImpl
import com.dibitara.app.data.repository.CustomInvestmentRepositoryImpl
import com.dibitara.app.data.repository.ExportRepositoryImpl
import com.dibitara.app.data.repository.ImportRepositoryImpl
import com.dibitara.app.data.repository.PatrimoineSnapshotRepositoryImpl
import com.dibitara.app.data.repository.RestoreRepositoryImpl
import com.dibitara.app.data.repository.VersementRepositoryImpl
import com.dibitara.app.domain.repository.*
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.ExportRepository
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.PatrimoineSnapshotRepository
import com.dibitara.app.domain.repository.RestoreRepository
import com.dibitara.app.domain.repository.VersementRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DibitaraDatabase =
        Room.databaseBuilder(context, DibitaraDatabase::class.java, "dibitara.db")
            .addMigrations(
                DibitaraDatabase.MIGRATION_1_2,
                DibitaraDatabase.MIGRATION_2_3,
                DibitaraDatabase.MIGRATION_3_4,
                DibitaraDatabase.MIGRATION_4_5,
                DibitaraDatabase.MIGRATION_5_6,
                DibitaraDatabase.MIGRATION_6_7,
                DibitaraDatabase.MIGRATION_7_8,
                DibitaraDatabase.MIGRATION_8_9,
                DibitaraDatabase.MIGRATION_9_10,
                DibitaraDatabase.MIGRATION_10_11,
                DibitaraDatabase.MIGRATION_11_12,
                DibitaraDatabase.MIGRATION_12_13,
                DibitaraDatabase.MIGRATION_13_14,
                DibitaraDatabase.MIGRATION_14_15,
                DibitaraDatabase.MIGRATION_15_16,
                DibitaraDatabase.MIGRATION_16_17,
                DibitaraDatabase.MIGRATION_17_18,
                DibitaraDatabase.MIGRATION_18_19
            )
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
            .build()

    @Provides fun provideTransactionDao(db: DibitaraDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: DibitaraDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideChildDao(db: DibitaraDatabase): ChildDao = db.childDao()
    @Provides fun provideDebtDao(db: DibitaraDatabase): DebtDao = db.debtDao()
    @Provides fun provideSavingsAccountDao(db: DibitaraDatabase): SavingsAccountDao = db.savingsAccountDao()
    @Provides fun provideRealEstateAssetDao(db: DibitaraDatabase): RealEstateAssetDao = db.realEstateAssetDao()
    @Provides fun provideScpiInvestmentDao(db: DibitaraDatabase): ScpiInvestmentDao = db.scpiInvestmentDao()
    @Provides fun provideAirbnbRentalDao(db: DibitaraDatabase): AirbnbRentalDao = db.airbnbRentalDao()
    @Provides fun provideCustomSubCategoryDao(db: DibitaraDatabase): CustomSubCategoryDao = db.customSubCategoryDao()
    @Provides fun provideMonthlyVersementDao(db: DibitaraDatabase): MonthlyVersementDao = db.monthlyVersementDao()
    @Provides fun providePreciousMetalDao(db: DibitaraDatabase): PreciousMetalDao = db.preciousMetalDao()
    @Provides fun provideCustomAssetDao(db: DibitaraDatabase): CustomAssetDao = db.customAssetDao()
    @Provides fun provideEmployeeSavingsDao(db: DibitaraDatabase): EmployeeSavingsDao = db.employeeSavingsDao()
    @Provides fun providePatrimoineSnapshotDao(db: DibitaraDatabase): PatrimoineSnapshotDao = db.patrimoineSnapshotDao()
    @Provides fun provideCategorizationRuleDao(db: DibitaraDatabase): CategorizationRuleDao = db.categorizationRuleDao()
    @Provides fun provideCategoryEnvelopeDao(db: DibitaraDatabase): CategoryEnvelopeDao = db.categoryEnvelopeDao()
    @Provides fun provideVehicleRentalEntryDao(db: DibitaraDatabase): VehicleRentalEntryDao = db.vehicleRentalEntryDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds abstract fun bindChildRepository(impl: ChildRepositoryImpl): ChildRepository
    @Binds abstract fun bindDebtRepository(impl: DebtRepositoryImpl): DebtRepository
    @Binds abstract fun bindSavingsRepository(impl: SavingsRepositoryImpl): SavingsRepository
    @Binds abstract fun bindInvestmentRepository(impl: InvestmentRepositoryImpl): InvestmentRepository
    @Binds abstract fun bindCustomSubCategoryRepository(impl: CustomSubCategoryRepositoryImpl): CustomSubCategoryRepository
    @Binds abstract fun bindVersementRepository(impl: VersementRepositoryImpl): VersementRepository
    @Binds abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository
    @Binds abstract fun bindCustomInvestmentRepository(impl: CustomInvestmentRepositoryImpl): CustomInvestmentRepository
    @Binds abstract fun bindImportRepository(impl: ImportRepositoryImpl): ImportRepository
    @Binds abstract fun bindPatrimoineSnapshotRepository(impl: PatrimoineSnapshotRepositoryImpl): PatrimoineSnapshotRepository
    @Binds abstract fun bindCategorizationRuleRepository(impl: CategorizationRuleRepositoryImpl): CategorizationRuleRepository
    @Binds abstract fun bindCategoryEnvelopeRepository(impl: CategoryEnvelopeRepositoryImpl): CategoryEnvelopeRepository
    @Binds abstract fun bindRestoreRepository(impl: RestoreRepositoryImpl): RestoreRepository
}
