package com.sans.finance.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.util.LocaleManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@HiltWorker
class SpendingRecapWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val localeManager: LocaleManager
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "daily_spending_recap"
        const val NOTIFICATION_ID = 9001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = cal.timeInMillis

            val summary = expenseDao.getTodaySpentSummary(startOfDay, endOfDay)
            val currency = localeManager.getCurrency()
            val totalSpent = summary.totalAmount ?: 0L
            val formattedSpent = CurrencyFormatter.formatAmount(totalSpent, currency)

            val title = "Today's Spending Recap"
            val contentText = if (summary.count > 0) {
                "Today: $formattedSpent across ${summary.count} expense${if (summary.count > 1) "s" else ""}."
            } else {
                "No expenses recorded today. Tap to add if you made any purchases!"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Spending Recap",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 9:00 PM summary of logged expenses"
            }
            notificationManager.createNotificationChannel(channel)

            // Main tap intent (view transactions)
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_VIEW_TRANSACTIONS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                901,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Add Expense quick action button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_ADD_TRANSACTION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                902,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_widget_receipt)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(mainPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_widget_add, "+ Add Expense", addPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
