package com.sans.expensetracker.di

import android.app.Application
import androidx.room.Room
import com.sans.expensetracker.data.local.AppDatabase
import com.sans.expensetracker.data.local.dao.ExpenseDao
import com.sans.expensetracker.data.preferences.AiPreferencesImpl
import com.sans.expensetracker.data.preferences.BudgetPreferencesImpl
import com.sans.expensetracker.data.repository.ExpenseRepositoryImpl
import com.sans.expensetracker.domain.preferences.AiPreferences
import com.sans.expensetracker.domain.preferences.BudgetPreferences
import com.sans.expensetracker.domain.repository.ExpenseRepository
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
            "expense_tracker_db"
        )
            .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .addCallback(callback)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabaseCallback(
        app: Application,
        categoryDao: javax.inject.Provider<com.sans.expensetracker.data.local.dao.CategoryDao>,
        expenseDao: javax.inject.Provider<ExpenseDao>,
        installmentDao: javax.inject.Provider<com.sans.expensetracker.data.local.dao.InstallmentDao>
    ): AppDatabase.Callback {
        return AppDatabase.Callback(app, categoryDao, expenseDao, installmentDao)
    }

    @Provides
    @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): com.sans.expensetracker.data.local.dao.CategoryDao =
        db.categoryDao

    @Provides
    @Singleton
    fun provideInstallmentDao(db: AppDatabase): com.sans.expensetracker.data.local.dao.InstallmentDao =
        db.installmentDao

    @Provides
    @Singleton
    fun provideTagDao(db: AppDatabase): com.sans.expensetracker.data.local.dao.TagDao = db.tagDao

    @Provides
    @Singleton
    fun provideExpenseRepository(
        dao: ExpenseDao,
        tagDao: com.sans.expensetracker.data.local.dao.TagDao,
        categoryDao: com.sans.expensetracker.data.local.dao.CategoryDao,
        installmentDao: com.sans.expensetracker.data.local.dao.InstallmentDao
    ): ExpenseRepository = ExpenseRepositoryImpl(dao, tagDao, categoryDao, installmentDao)

    @Provides
    @Singleton
    fun provideInstallmentRepository(dao: com.sans.expensetracker.data.local.dao.InstallmentDao): com.sans.expensetracker.domain.repository.InstallmentRepository =
        com.sans.expensetracker.data.repository.InstallmentRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideLocaleManager(app: Application): com.sans.expensetracker.data.util.LocaleManager =
        com.sans.expensetracker.data.util.LocaleManager(app)

    @Provides
    @Singleton
    fun provideBudgetPreferences(app: Application): BudgetPreferences = BudgetPreferencesImpl(app)

    @Provides
    @Singleton
    fun provideAiPreferences(app: Application): AiPreferences = AiPreferencesImpl(app)
}
