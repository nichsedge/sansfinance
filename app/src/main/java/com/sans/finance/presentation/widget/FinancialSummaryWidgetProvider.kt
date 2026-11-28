package com.sans.finance.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.usecase.GetDashboardSummaryUseCase
import com.sans.finance.domain.usecase.GetWealthMetricsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class FinancialSummaryWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SummaryEntryPoint {
        fun getDashboardSummaryUseCase(): GetDashboardSummaryUseCase
        fun getWealthMetricsUseCase(): GetWealthMetricsUseCase
        fun localeManager(): LocaleManager
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SummaryEntryPoint::class.java
                )
                val getDashboardSummaryUseCase = entryPoint.getDashboardSummaryUseCase()
                val getWealthMetricsUseCase = entryPoint.getWealthMetricsUseCase()

                val summary = getDashboardSummaryUseCase().first()
                val metrics = getWealthMetricsUseCase().first()
                val baseCurrency = summary.currentCurrency

                // --- Net Worth ---
                val formattedNetWorth = CurrencyFormatter.formatAmount(summary.netWorth, baseCurrency)

                // --- Budget & Runway ---
                val budgetPct = if (summary.globalBudget > 0) {
                    ((summary.globalSpent.toDouble() / summary.globalBudget) * 100).toInt()
                } else {
                    0
                }

                val budgetText = if (summary.globalBudget == 0L) {
                    context.getString(R.string.widget_no_budget_set)
                } else {
                    val formattedSpent = CurrencyFormatter.formatAmountCompact(summary.globalSpent, baseCurrency)
                    val formattedBudget = CurrencyFormatter.formatAmountCompact(summary.globalBudget, baseCurrency)
                    "$formattedSpent / $formattedBudget"
                }

                val budgetPctText = if (summary.globalBudget > 0L) "$budgetPct%" else ""

                // --- Status Row (Velocity & Runway) ---
                val statusText = when {
                    summary.spendingVelocity > 1.1f -> "⚠️ Spending too fast"
                    metrics.runwayMonths > 0 -> "🛡️ ${String.format(java.util.Locale.US, "%.2f", metrics.runwayMonths)} mo runway"
                    else -> context.getString(R.string.widget_no_transactions_today)
                }

                val dateStr = DateFormatterUtils.formatDayMonth(System.currentTimeMillis())

                // --- Build RemoteViews ---
                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_financial_summary).apply {
                        setTextViewText(R.id.widget_summary_net_worth, formattedNetWorth)
                        setTextViewText(R.id.widget_summary_budget_text, budgetText)
                        setTextViewText(R.id.widget_summary_budget_pct, budgetPctText)
                        setProgressBar(R.id.widget_summary_progress, 100, budgetPct.coerceIn(0, 100), false)
                        setTextViewText(R.id.widget_summary_today, statusText)
                        setTextViewText(R.id.widget_summary_date, dateStr)

                        // 1. Add Transaction PendingIntent
                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_ADD_TRANSACTION
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val addPendingIntent = PendingIntent.getActivity(
                            context,
                            3001,
                            addIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_summary_btn_add, addPendingIntent)

                        // 2. View Dashboard PendingIntent
                        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val dashboardPendingIntent = PendingIntent.getActivity(
                            context,
                            3002,
                            dashboardIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_summary_btn_view, dashboardPendingIntent)
                        setOnClickPendingIntent(R.id.widget_summary_header, dashboardPendingIntent)
                        setOnClickPendingIntent(R.id.widget_summary_body, dashboardPendingIntent)
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                com.sans.finance.presentation.widget.glance.FinancialSummaryGlanceReceiver.updateAll(context)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, FinancialSummaryWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (widgetIds.isNotEmpty()) {
                    val intent = Intent(context, FinancialSummaryWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
