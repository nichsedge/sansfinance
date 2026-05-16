package com.sans.finance.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.DatabaseMigrations
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.repository.ExpenseRepositoryImpl
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.data.ai.AiSettingsRepository
import com.sans.finance.data.ai.SecureAiSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient


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
            .addMigrations(*DatabaseMigrations.ALL)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL(
                        """
                        INSERT INTO account_types (name, icon, isLiability, display_order, createdAt)
                        SELECT a.type, 'AccountBalance', 0, 0, strftime('%s','now') * 1000
                        FROM (SELECT DISTINCT TRIM(type) AS type FROM accounts WHERE TRIM(type) <> '') a
                        WHERE NOT EXISTS (SELECT 1 FROM account_types)
                        """.trimIndent()
                    )
                }
            })
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
    fun provideAccountTypeDao(db: AppDatabase): com.sans.finance.data.local.dao.AccountTypeDao =
        db.accountTypeDao

    @Provides
    @Singleton
    fun provideAccountTypeRepository(dao: com.sans.finance.data.local.dao.AccountTypeDao): com.sans.finance.domain.repository.AccountTypeRepository =
        com.sans.finance.data.repository.AccountTypeRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideAccountAliasDao(db: AppDatabase): com.sans.finance.data.local.dao.AccountAliasDao =
        db.accountAliasDao

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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideLocaleManager(app: Application): com.sans.finance.data.util.LocaleManager =
        com.sans.finance.data.util.LocaleManager(app)

    @Provides
    @Singleton
    fun provideAiSettingsRepository(repo: SecureAiSettingsRepository): AiSettingsRepository = repo

}
