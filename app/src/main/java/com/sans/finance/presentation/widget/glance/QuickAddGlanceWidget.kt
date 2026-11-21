package com.sans.finance.presentation.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.util.LocaleManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Calendar

class QuickAddGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun expenseDao(): ExpenseDao
        fun localeManager(): LocaleManager
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        var formattedTotal = "--"
        var countText = context.getString(R.string.widget_no_transactions_today)

        try {
            val expenseDao = entryPoint.expenseDao()
            val localeManager = entryPoint.localeManager()

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
            val totalSpentCents = summary.totalAmount ?: 0L
            val currency = localeManager.getCurrency()
            formattedTotal = CurrencyFormatter.formatAmount(totalSpentCents, currency)

            countText = if (summary.count == 0) {
                context.getString(R.string.widget_no_transactions_today)
            } else {
                context.getString(R.string.widget_transactions_today_count, summary.count)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TRANSACTION
            putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, "EXPENSE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val addIncomeIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TRANSACTION
            putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, "INCOME")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val historyIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_TRANSACTIONS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(20.dp)
                        .padding(14.dp)
                        .clickable(actionStartActivity(mainIntent))
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TODAY'S SPENDING",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.secondary
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                text = countText,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = GlanceTheme.colors.onSurfaceVariant
                                )
                            )
                        }

                        // Amount
                        Text(
                            text = formattedTotal,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            ),
                            modifier = GlanceModifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Quick Action Buttons
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(12.dp)
                                    .padding(vertical = 8.dp)
                                    .clickable(actionStartActivity(addExpenseIntent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "- Expense",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onPrimary
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(GlanceTheme.colors.secondaryContainer)
                                    .cornerRadius(12.dp)
                                    .padding(vertical = 8.dp)
                                    .clickable(actionStartActivity(addIncomeIntent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ Income",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onSecondaryContainer
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Box(
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.surfaceVariant)
                                    .cornerRadius(12.dp)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .clickable(actionStartActivity(historyIntent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "List",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
