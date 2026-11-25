package com.sans.finance.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.SyncEmailExpensesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

private const val TAG = "EmailExpensesSyncWorker"

/**
 * Resource-efficient background worker that syncs staged email expenses once daily
 * when internet connectivity is available (enforced via WorkManager Constraints).
 */
@HiltWorker
class EmailExpensesSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncEmailExpensesUseCase: SyncEmailExpensesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "email_auto_detect_expenses"
        const val NOTIFICATION_ID = 9002
    }

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.userPreferences.first()
        if (!prefs.emailSyncEnabled || prefs.syncGatewayUrl.isBlank()) {
            return Result.success()
        }

        return syncEmailExpensesUseCase().fold(
            onSuccess = { summary ->
                Log.i(
                    TAG,
                    "Email sync complete: inserted=${summary.insertedCount}, duplicates=${summary.duplicateCount}, empty=${summary.wasEmpty}"
                )
                if (summary.insertedCount > 0) {
                    postSyncNotification(summary.insertedCount)
                }
                Result.success()
            },
            onFailure = { err ->
                Log.e(TAG, "Email sync failed, scheduling retry", err)
                Result.retry()
            }
        )
    }

    private fun postSyncNotification(insertedCount: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto-Detected Expenses",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when new email receipts or bank alerts are imported"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_TRANSACTIONS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            902,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Imported $insertedCount new transaction${if (insertedCount > 1) "s" else ""} from email. Tap to review."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Auto-Detected Expenses")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping notification", e)
        }
    }
}
