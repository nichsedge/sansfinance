package com.sans.finance

import android.app.Application
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sans.finance.domain.repository.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SansFinanceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var localeManager: com.sans.finance.data.util.LocaleManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSync()
        scheduleDailyRecap()
        applicationScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            if (prefs.enabledCurrencies.isNotEmpty()) {
                localeManager.setEnabledCurrencies(prefs.enabledCurrencies)
            }
            if (prefs.baseCurrency.isNotBlank()) {
                localeManager.setCurrency(prefs.baseCurrency)
            }
            rescheduleBackupWork(this@SansFinanceApp, localeManager, userPreferencesRepository)
            rescheduleEmailSyncWork(this@SansFinanceApp, userPreferencesRepository, forceUpdate = false)
        }
    }

    private fun scheduleDailyRecap() {
        val currentCal = java.util.Calendar.getInstance()
        val targetCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 21)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (targetCal.before(currentCal)) {
            targetCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelayMillis = targetCal.timeInMillis - currentCal.timeInMillis

        val recapRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SpendingRecapWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySpendingRecap",
            ExistingPeriodicWorkPolicy.KEEP,
            recapRequest
        )
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRatesRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SyncExchangeRatesWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncExchangeRates",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRatesRequest
        )
    }

    companion object {
        fun rescheduleBackupWork(
            context: Context,
            localeManager: com.sans.finance.data.util.LocaleManager,
            userPreferencesRepository: UserPreferencesRepository
        ) {
            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            applicationScope.launch {
                val prefs = userPreferencesRepository.userPreferences.first()
                val frequency = prefs.backupFrequency
                val wifiOnly = prefs.backupWifiOnly
                val requiresCharging = prefs.backupRequiresCharging

                val workManager = WorkManager.getInstance(context)

                if (frequency == "OFF" || frequency == "MANUAL") {
                    workManager.cancelUniqueWork("CloudSyncAndBackup")
                    return@launch
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
                    )
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                        .build()

                workManager.enqueueUniquePeriodicWork(
                    "CloudSyncAndBackup",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    cloudSyncRequest
                )
            }
        }

        fun rescheduleEmailSyncWork(
            context: Context,
            userPreferencesRepository: UserPreferencesRepository,
            forceUpdate: Boolean = true
        ) {
            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            applicationScope.launch {
                val prefs = userPreferencesRepository.userPreferences.first()
                val workManager = WorkManager.getInstance(context)

                if (!prefs.emailSyncEnabled) {
                    workManager.cancelUniqueWork("EmailExpensesAutoSync")
                    return@launch
                }

                val currentCal = java.util.Calendar.getInstance()
                val targetCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, prefs.emailSyncHour.coerceIn(0, 23))
                    set(java.util.Calendar.MINUTE, prefs.emailSyncMinute.coerceIn(0, 59))
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                if (targetCal.before(currentCal)) {
                    targetCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                val initialDelayMillis = (targetCal.timeInMillis - currentCal.timeInMillis).coerceAtLeast(0L)

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(
                        if (prefs.emailSyncWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                    )
                    .build()

                val emailSyncRequest =
                    PeriodicWorkRequestBuilder<com.sans.finance.data.worker.EmailExpensesSyncWorker>(
                        1, TimeUnit.DAYS
                    )
                        .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                        .build()

                workManager.enqueueUniquePeriodicWork(
                    "EmailExpensesAutoSync",
                    if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                    emailSyncRequest
                )
            }
        }
    }
}
