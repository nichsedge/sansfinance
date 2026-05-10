package com.sans.finance.di

import android.app.Application
import androidx.room.Room
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.preferences.AiPreferencesImpl
import com.sans.finance.data.preferences.BudgetPreferencesImpl
import com.sans.finance.data.repository.ExpenseRepositoryImpl
import com.sans.finance.domain.preferences.AiPreferences
import com.sans.finance.domain.preferences.BudgetPreferences
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
        app: Application,
        callback: AppDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "sans_finance_db"
        )
            .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .addCallback(callback)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabaseCallback(
        app: Application,
        categoryDao: javax.inject.Provider<com.sans.finance.data.local.dao.CategoryDao>,
        expenseDao: javax.inject.Provider<ExpenseDao>,
        installmentDao: javax.inject.Provider<com.sans.finance.data.local.dao.InstallmentDao>
    ): AppDatabase.Callback {
        return AppDatabase.Callback(app, categoryDao, expenseDao, installmentDao)
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
    fun provideAccountDao(db: AppDatabase): com.sans.finance.data.local.dao.AccountDao = db.accountDao

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
    fun provideExpenseRepository(
        dao: ExpenseDao,
        tagDao: com.sans.finance.data.local.dao.TagDao,
        categoryDao: com.sans.finance.data.local.dao.CategoryDao,
        installmentDao: com.sans.finance.data.local.dao.InstallmentDao,
        accountDao: com.sans.finance.data.local.dao.AccountDao
    ): ExpenseRepository = ExpenseRepositoryImpl(dao, tagDao, categoryDao, installmentDao, accountDao)

    @Provides
    @Singleton
    fun provideInstallmentRepository(dao: com.sans.finance.data.local.dao.InstallmentDao): com.sans.finance.domain.repository.InstallmentRepository =
        com.sans.finance.data.repository.InstallmentRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideLocaleManager(app: Application): com.sans.finance.data.util.LocaleManager =
        com.sans.finance.data.util.LocaleManager(app)

    @Provides
    @Singleton
    fun provideBudgetPreferences(app: Application): BudgetPreferences = BudgetPreferencesImpl(app)

    @Provides
    @Singleton
    fun provideAiPreferences(app: Application): AiPreferences = AiPreferencesImpl(app)
}
