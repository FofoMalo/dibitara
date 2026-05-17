package com.dibitara.app.di

import android.content.Context
import androidx.room.Room
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.dao.MonthlyVersementDao
import com.dibitara.app.data.repository.*
import com.dibitara.app.data.repository.VersementRepositoryImpl
import com.dibitara.app.domain.repository.*
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
                DibitaraDatabase.MIGRATION_6_7
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
}
