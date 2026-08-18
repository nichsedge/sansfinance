package com.sans.finance

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SansFinanceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var localeManager: com.sans.finance.data.util.LocaleManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSync()
        rescheduleBackupWork(this, localeManager)
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRatesRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SyncExchangeRatesWorker>(
                24, TimeUnit.HOURS
            ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncExchangeRates",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRatesRequest
        )
    }

    companion object {
        fun rescheduleBackupWork(
            context: Context,
            localeManager: com.sans.finance.data.util.LocaleManager
        ) {
            val frequency = localeManager.getBackupFrequency()
            val wifiOnly = localeManager.isBackupWifiOnly()
            val requiresCharging = localeManager.isBackupRequiresCharging()

            val workManager = WorkManager.getInstance(context)

            if (frequency == "OFF" || frequency == "MANUAL") {
                workManager.cancelUniqueWork("CloudSyncAndBackup")
                return
            }

            val intervalDays = when (frequency) {
                "DAILY" -> 1L
                "WEEKLY" -> 7L
                "MONTHLY" -> 30L
                else -> 7L
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(requiresCharging)
                .build()

            val cloudSyncRequest =
                PeriodicWorkRequestBuilder<com.sans.finance.data.worker.CloudSyncAndBackupWorker>(
                    intervalDays, TimeUnit.DAYS
                ).setConstraints(constraints).build()

            workManager.enqueueUniquePeriodicWork(
                "CloudSyncAndBackup",
                ExistingPeriodicWorkPolicy.UPDATE,
                cloudSyncRequest
            )
        }
    }
}
