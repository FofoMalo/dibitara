package com.dibitara.app.di

import android.content.Context
import androidx.room.Room
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.dao.BudgetDao
import com.dibitara.app.data.local.dao.TransactionDao
import com.dibitara.app.data.repository.BudgetRepositoryImpl
import com.dibitara.app.data.repository.TransactionRepositoryImpl
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt : indique à l'injecteur comment construire chaque dépendance.
 * @Provides = Hilt appelle cette fonction pour créer l'objet.
 * @Singleton = une seule instance partagée dans toute l'app.
 *
 * TODO Sprint 1 : remplacer Room.databaseBuilder par une version chiffrée (SQLCipher).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DibitaraDatabase =
        Room.databaseBuilder(context, DibitaraDatabase::class.java, "dibitara.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideTransactionDao(db: DibitaraDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: DibitaraDatabase): BudgetDao = db.budgetDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository
}
