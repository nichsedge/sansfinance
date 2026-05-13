package com.sans.finance

import android.app.Application
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSync()
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SyncExchangeRatesWorker>(
                24, TimeUnit.HOURS
            ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncExchangeRates",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
