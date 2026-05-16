package com.sans.finance.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openedNames = mutableListOf<String>()

    @After
    fun tearDown() {
        openedNames.forEach { context.deleteDatabase(it) }
        openedNames.clear()
    }

    @Test
    fun migration25To27_addsAccountColumnsAndPortfolioTargetsTable() {
        val db = openDatabase("migration_25_27_test.db")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `balance` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        DatabaseMigrations.MIGRATION_25_27.migrate(db)

        assertHasColumn(db, "accounts", "interest_rate")
        assertHasColumn(db, "accounts", "min_payment")
        assertTableExists(db, "portfolio_targets")
    }

    @Test
    fun migration27To28_createsAccountTypesTable() {
        val db = openDatabase("migration_27_28_test.db")

        DatabaseMigrations.MIGRATION_27_28.migrate(db)

        assertTableExists(db, "account_types")
        assertHasColumn(db, "account_types", "name")
        assertHasColumn(db, "account_types", "isLiability")
    }

    @Test
    fun migration28To29_recreatesPortfolioHoldingsWithAccountColumns() {
        val db = openDatabase("migration_28_29_test.db")
        db.execSQL("CREATE TABLE IF NOT EXISTS `portfolio_snapshot_headers` (`snapshotDate` INTEGER PRIMARY KEY NOT NULL, `source` TEXT NOT NULL, `capturedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` INTEGER NOT NULL, `currency` TEXT NOT NULL, `interest_rate` REAL NOT NULL DEFAULT 0.0, `min_payment` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `portfolio_holdings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `snapshot_date` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `asset` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `price` REAL,
                `value_idr` REAL NOT NULL,
                `asset_class` TEXT NOT NULL,
                `account` TEXT NOT NULL,
                `details` TEXT
            )
            """.trimIndent()
        )

        DatabaseMigrations.MIGRATION_28_29.migrate(db)

        assertHasColumn(db, "portfolio_holdings", "account_id")
        assertHasColumn(db, "portfolio_holdings", "account_key")
        assertHasColumn(db, "portfolio_holdings", "account_name")
    }

    @Test
    fun migration29To30_createsAccountAliasesTable() {
        val db = openDatabase("migration_29_30_test.db")

        DatabaseMigrations.MIGRATION_29_30.migrate(db)

        assertTableExists(db, "account_aliases")
        assertHasColumn(db, "account_aliases", "accountKey")
        assertHasColumn(db, "account_aliases", "aliasName")
    }

    @Test
    fun migration30To31_addsDisplayOrderColumns() {
        val db = openDatabase("migration_30_31_test.db")
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` INTEGER NOT NULL, `currency` TEXT NOT NULL, `interest_rate` REAL NOT NULL DEFAULT 0.0, `min_payment` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `account_types` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `isLiability` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)")

        DatabaseMigrations.MIGRATION_30_31.migrate(db)

        assertHasColumn(db, "accounts", "display_order")
        assertHasColumn(db, "account_types", "display_order")
    }

    private fun openDatabase(name: String): SupportSQLiteDatabase {
        openedNames += name
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper.writableDatabase
    }

    private fun assertTableExists(db: SupportSQLiteDatabase, tableName: String) {
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
        cursor.use {
            assertTrue("Expected table $tableName to exist", it.moveToFirst())
        }
    }

    private fun assertHasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String) {
        val cursor = db.query("PRAGMA table_info(`$tableName`)")
        cursor.use {
            var found = false
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == columnName) {
                    found = true
                    break
                }
            }
            assertTrue("Expected column $columnName in table $tableName", found)
        }
    }
}
