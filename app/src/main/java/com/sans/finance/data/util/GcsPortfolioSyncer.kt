package com.sans.finance.data.util

import android.content.Context
import com.sans.finance.data.local.entity.PortfolioHoldingEntity

object GcsPortfolioSyncer {

    // Direct 1-shot fetch of latest snapshot (delegates to CloudStorageSyncer)
    suspend fun downloadLatestSnapshot(context: Context): Triple<Long, List<PortfolioHoldingEntity>, Double?> {
        return CloudStorageSyncer.downloadLatestSnapshot(context)
    }

    // Direct upload of SQLite database (delegates to CloudStorageSyncer)
    suspend fun uploadDatabaseBackup(context: Context, dbFile: java.io.File): Result<String> {
        return CloudStorageSyncer.uploadDatabaseBackup(context, dbFile)
    }
}

