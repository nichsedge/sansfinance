package com.sans.finance.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.util.GcsPortfolioSyncer
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CloudSyncWorker"

@HiltWorker
class CloudSyncAndBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val portfolioRepository: PortfolioRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var uploadSuccess = false
        var syncSuccess = false

        // 1. Upload SQLite Database Backup to GCS
        val snapshotFile = java.io.File(context.cacheDir, "sans_finance_backup.sqlite")
        try {
            db.createBackupSnapshot(snapshotFile)
            if (snapshotFile.exists() && snapshotFile.length() > 0) {
                val fileSize = snapshotFile.length()
                val uploadResult = GcsPortfolioSyncer.uploadDatabaseBackup(context, snapshotFile)
                uploadResult.fold(
                    onSuccess = { msg ->
                        Log.i(TAG, "Database successfully backed up to GCS: $msg")
                        localeManager.setLastBackupTime(System.currentTimeMillis())
                        localeManager.setLastBackupSizeBytes(fileSize)
                        uploadSuccess = true
                    },
                    onFailure = { err ->
                        Log.e(TAG, "Failed to upload database to GCS", err)
                    }
                )
            } else {
                Log.w(TAG, "Snapshot file creation failed or is empty at ${snapshotFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during GCS database backup", e)
        } finally {
            if (snapshotFile.exists()) {
                snapshotFile.delete()
            }
        }

        // 2. Download and Import Latest Portfolio Snapshot from GCS
        try {
            val (date, items, exchangeRate) = GcsPortfolioSyncer.downloadLatestSnapshot(context)
            if (items.isNotEmpty()) {
                portfolioRepository.importSnapshot(date, items, exchangeRate)
                Log.i(TAG, "Successfully synced ${items.size} portfolio holdings from GCS (snapshot: $date)")
                syncSuccess = true
            } else {
                Log.w(TAG, "No portfolio items found in GCS snapshot")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during GCS portfolio sync", e)
        }

        if (uploadSuccess || syncSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
