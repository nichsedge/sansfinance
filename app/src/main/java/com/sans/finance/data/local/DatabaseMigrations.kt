package com.sans.finance.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_25_27 = object : Migration(25, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `interest_rate` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `min_payment` INTEGER NOT NULL DEFAULT 0")
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

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `account_types` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `icon` TEXT NOT NULL,
                    `isLiability` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `portfolio_holdings_new` (
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
                    `account_id` INTEGER,
                    `account_key` TEXT,
                    `account_name` TEXT,
                    `account` TEXT NOT NULL,
                    `details` TEXT,
                    FOREIGN KEY(`snapshot_date`) REFERENCES `portfolio_snapshot_headers`(`snapshotDate`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `portfolio_holdings_new` (
                    `id`,`snapshot_date`,`source`,`category`,`asset`,`currency`,
                    `quantity`,`price`,`value_idr`,`asset_class`,`account_id`,
                    `account_key`,`account_name`,`account`,`details`
                )
                SELECT
                    `id`,`snapshot_date`,`source`,`category`,`asset`,`currency`,
                    `quantity`,`price`,`value_idr`,`asset_class`,NULL,
                    NULL,NULL,`account`,`details`
                FROM `portfolio_holdings`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `portfolio_holdings`")
            db.execSQL("ALTER TABLE `portfolio_holdings_new` RENAME TO `portfolio_holdings`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_holdings_snapshot_date` ON `portfolio_holdings` (`snapshot_date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_holdings_account_id` ON `portfolio_holdings` (`account_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_holdings_account_key` ON `portfolio_holdings` (`account_key`)")
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `account_aliases` (
                    `accountKey` TEXT NOT NULL,
                    `aliasName` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`accountKey`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `display_order` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `account_types` ADD COLUMN `display_order` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tags` ADD COLUMN `isVisible` INTEGER NOT NULL DEFAULT 1")
        }
    }

    val ALL = arrayOf(
        MIGRATION_25_27,
        MIGRATION_27_28,
        MIGRATION_28_29,
        MIGRATION_29_30,
        MIGRATION_30_31,
        MIGRATION_31_32
    )
}
