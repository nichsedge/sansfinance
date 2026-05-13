package com.sans.finance.di

import android.app.Application
import androidx.room.Room
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.repository.ExpenseRepositoryImpl
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application
    ): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "sans_finance_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23,
                AppDatabase.MIGRATION_23_24
            )
            .build()
    }


    @Provides
    @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): com.sans.finance.data.local.dao.CategoryDao =
        db.categoryDao

    @Provides
    @Singleton
    fun provideInstallmentDao(db: AppDatabase): com.sans.finance.data.local.dao.InstallmentDao =
        db.installmentDao

    @Provides
    @Singleton
    fun provideTagDao(db: AppDatabase): com.sans.finance.data.local.dao.TagDao = db.tagDao

    @Provides
    @Singleton
    fun provideAccountDao(db: AppDatabase): com.sans.finance.data.local.dao.AccountDao =
        db.accountDao

    @Provides
    @Singleton
    fun provideAccountRepository(dao: com.sans.finance.data.local.dao.AccountDao): com.sans.finance.domain.repository.AccountRepository =
        com.sans.finance.data.repository.AccountRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): com.sans.finance.data.local.dao.GoalDao = db.goalDao

    @Provides
    @Singleton
    fun provideGoalRepository(dao: com.sans.finance.data.local.dao.GoalDao): com.sans.finance.domain.repository.GoalRepository =
        com.sans.finance.data.repository.GoalRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): com.sans.finance.data.local.dao.BudgetDao = db.budgetDao

    @Provides
    @Singleton
    fun provideBudgetRepository(dao: com.sans.finance.data.local.dao.BudgetDao): com.sans.finance.domain.repository.BudgetRepository =
        com.sans.finance.data.repository.BudgetRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePortfolioDao(db: AppDatabase): com.sans.finance.data.local.dao.PortfolioDao =
        db.portfolioDao

    @Provides
    @Singleton
    fun provideCurrencyDao(db: AppDatabase): com.sans.finance.data.local.dao.CurrencyDao =
        db.currencyDao

    @Provides
    @Singleton
    fun providePortfolioRepository(dao: com.sans.finance.data.local.dao.PortfolioDao): com.sans.finance.domain.repository.PortfolioRepository =
        com.sans.finance.data.repository.PortfolioRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCurrencyRepository(dao: com.sans.finance.data.local.dao.CurrencyDao): com.sans.finance.domain.repository.CurrencyRepository =
        com.sans.finance.data.repository.CurrencyRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideExpenseRepository(
        db: AppDatabase,
        dao: ExpenseDao,
        tagDao: com.sans.finance.data.local.dao.TagDao,
        categoryDao: com.sans.finance.data.local.dao.CategoryDao,
        installmentDao: com.sans.finance.data.local.dao.InstallmentDao,
        accountDao: com.sans.finance.data.local.dao.AccountDao
    ): ExpenseRepository =
        ExpenseRepositoryImpl(db, dao, tagDao, categoryDao, installmentDao, accountDao)

    @Provides
    @Singleton
    fun provideInstallmentRepository(dao: com.sans.finance.data.local.dao.InstallmentDao): com.sans.finance.domain.repository.InstallmentRepository =
        com.sans.finance.data.repository.InstallmentRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideLocaleManager(app: Application): com.sans.finance.data.util.LocaleManager =
        com.sans.finance.data.util.LocaleManager(app)

}
