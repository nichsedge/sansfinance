package com.sans.finance.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_25_27 = object : Migration(25, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Update accounts table
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `interest_rate` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `min_payment` INTEGER NOT NULL DEFAULT 0")

            // 2. Create portfolio_targets table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `portfolio_targets` (
                    `assetClass` TEXT NOT NULL,
                    `targetPercentage` REAL NOT NULL,
                    `description` TEXT NOT NULL DEFAULT '',
                    `riskLevel` TEXT NOT NULL DEFAULT 'MEDIUM',
                    PRIMARY KEY(`assetClass`)
                )
                """.trimIndent()
            )
        }
    }

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
            .addMigrations(MIGRATION_25_27)
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
    fun providePortfolioTargetDao(db: AppDatabase): com.sans.finance.data.local.dao.PortfolioTargetDao =
        db.portfolioTargetDao

    @Provides
    @Singleton
    fun provideCurrencyDao(db: AppDatabase): com.sans.finance.data.local.dao.CurrencyDao =
        db.currencyDao

    @Provides
    @Singleton
    fun providePortfolioRepository(
        dao: com.sans.finance.data.local.dao.PortfolioDao,
        targetDao: com.sans.finance.data.local.dao.PortfolioTargetDao,
        expenseDao: com.sans.finance.data.local.dao.ExpenseDao,
        accountDao: com.sans.finance.data.local.dao.AccountDao
    ): com.sans.finance.domain.repository.PortfolioRepository =
        com.sans.finance.data.repository.PortfolioRepositoryImpl(dao, targetDao, expenseDao, accountDao)

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
