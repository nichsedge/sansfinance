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

    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `fx_rates` (
                    `currency_pair` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `rate` REAL NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`currency_pair`, `date`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date_type_is_installment` ON `expenses` (`date`, `type`, `is_installment`)")
        }
    }

    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `investment_metadata` (
                    `code` TEXT NOT NULL,
                    `rate` REAL NOT NULL,
                    `type` TEXT NOT NULL DEFAULT 'SUKUK',
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`code`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_title` ON `expenses` (`title`)")

            // Seed initial Sukuk rates
            val now = System.currentTimeMillis()
            db.execSQL("INSERT OR IGNORE INTO investment_metadata (code, rate, type, updatedAt) VALUES ('ST010T4', 0.0640, 'SUKUK', $now)")
            db.execSQL("INSERT OR IGNORE INTO investment_metadata (code, rate, type, updatedAt) VALUES ('ST012T4', 0.0655, 'SUKUK', $now)")
            db.execSQL("INSERT OR IGNORE INTO investment_metadata (code, rate, type, updatedAt) VALUES ('ST013T2', 0.0640, 'SUKUK', $now)")
            db.execSQL("INSERT OR IGNORE INTO investment_metadata (code, rate, type, updatedAt) VALUES ('ST014T2', 0.0640, 'SUKUK', $now)")
        }
    }

    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_items_due_date_status` ON `installment_items` (`due_date`, `status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_items_status_due_date` ON `installment_items` (`status`, `due_date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_is_recurring_date` ON `expenses` (`is_recurring`, `date`)")
        }
    }

    /**
     * Migration 36→37: Clean up synthetic installment ID collision.
     *
     * Prior versions used `id + 100_000_000` as synthetic IDs for installment payment items.
     * SQLite autoincrement surpassed this offset, causing real expenses to be assigned IDs
     * in the synthetic range (100_000_000+). This migration:
     *
     * 1. Deletes any orphaned synthetic rows that leaked into the expenses table.
     * 2. Reassigns real expense IDs that ended up above the old offset threshold
     *    back to sequential values, updating expense_tag_ref foreign keys.
     * 3. Resets sqlite_sequence for expenses to MAX(id) after reassignment.
     */
    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val oldOffset = 100_000_000L

            // Step 1: Delete orphaned synthetic rows (id >= offset AND maps to installment_item)
            db.execSQL(
                "DELETE FROM expenses WHERE id >= $oldOffset AND (id - $oldOffset) IN (SELECT id FROM installment_items)"
            )

            // Step 2: Find the max real ID below the old offset range
            val cursor = db.query("SELECT COALESCE(MAX(id), 0) FROM expenses WHERE id < $oldOffset")
            var nextId = 1L
            if (cursor.moveToFirst()) {
                nextId = cursor.getLong(0) + 1
            }
            cursor.close()

            // Step 3: Reassign IDs for real expenses stuck in the synthetic range
            val realExpCursor = db.query("SELECT id FROM expenses WHERE id >= $oldOffset ORDER BY id ASC")
            while (realExpCursor.moveToNext()) {
                val oldId = realExpCursor.getLong(0)
                val newId = nextId++
                db.execSQL("UPDATE expenses SET id = $newId WHERE id = $oldId")
                db.execSQL("UPDATE expense_tag_ref SET expenseId = $newId WHERE expenseId = $oldId")
            }
            realExpCursor.close()

            // Step 4: Reset autoincrement sequence to the new max id
            val maxIdCursor = db.query("SELECT COALESCE(MAX(id), 0) FROM expenses")
            var maxId = 0L
            if (maxIdCursor.moveToFirst()) {
                maxId = maxIdCursor.getLong(0)
            }
            maxIdCursor.close()
            db.execSQL("UPDATE sqlite_sequence SET seq = $maxId WHERE name = 'expenses'")

            // Step 5: Rebuild FTS index to reflect the reassigned IDs
            db.execSQL("DELETE FROM expenses_fts")
            db.execSQL("INSERT INTO expenses_fts(expenses_fts) VALUES('rebuild')")
        }
    }

    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrence_end_type` TEXT DEFAULT 'NEVER'")
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrence_end_date` INTEGER")
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrence_total_occurrences` INTEGER")
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrence_interval_multiplier` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrence_status` TEXT NOT NULL DEFAULT 'ACTIVE'")
        }
    }

    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `portfolio_holdings` ADD COLUMN `cost_basis` REAL")
            db.execSQL("ALTER TABLE `account_types` ADD COLUMN `is_investment` INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE `account_types` 
                SET `is_investment` = 1 
                WHERE LOWER(name) IN ('investment', 'rdn', 'broker', 'stocks', 'crypto', 'p2p lending')
                """.trimIndent()
            )
        }
    }

    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `portfolio_holdings` ADD COLUMN `yield_rate` REAL")
        }
    }

    val ALL = arrayOf(
        MIGRATION_25_27,
        MIGRATION_27_28,
        MIGRATION_28_29,
        MIGRATION_29_30,
        MIGRATION_30_31,
        MIGRATION_31_32,
        MIGRATION_32_33,
        MIGRATION_33_34,
        MIGRATION_34_35,
        MIGRATION_35_36,
        MIGRATION_36_37,
        MIGRATION_37_38,
        MIGRATION_38_39,
        MIGRATION_39_40
    )
}
