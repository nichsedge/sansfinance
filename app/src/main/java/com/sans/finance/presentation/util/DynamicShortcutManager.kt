package com.sans.finance.presentation.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.data.local.dao.CategoryDao
import com.sans.finance.data.local.dao.ExpenseDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object DynamicShortcutManager {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ShortcutEntryPoint {
        fun expenseDao(): ExpenseDao
        fun categoryDao(): CategoryDao
    }

    fun updateShortcuts(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ShortcutEntryPoint::class.java
                )
                val expenseDao = entryPoint.expenseDao()
                val categoryDao = entryPoint.categoryDao()

                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis

                val expenses = expenseDao.getAllExpenses().first()
                val topCategoryCounts = expenses
                    .filter { it.expense.date >= startOfMonth && it.expense.type == "EXPENSE" }
                    .groupBy { it.expense.categoryId }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)

                val allCategories = categoryDao.getAllCategoriesSync().associateBy { it.id }

                val shortcuts = topCategoryCounts.mapNotNull { (catId, _) ->
                    val category = allCategories[catId] ?: return@mapNotNull null
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_ADD_TRANSACTION
                        putExtra(MainActivity.EXTRA_CATEGORY_ID, category.id)
                        putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, "EXPENSE")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    ShortcutInfo.Builder(context, "shortcut_cat_${category.id}")
                        .setShortLabel(category.name)
                        .setLongLabel("Log ${category.name}")
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_add_expense))
                        .setIntent(intent)
                        .build()
                }

                if (shortcuts.isNotEmpty()) {
                    shortcutManager.dynamicShortcuts = shortcuts
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isPinShortcutSupported(context: Context): Boolean {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        return shortcutManager.isRequestPinShortcutSupported
    }

    fun requestPinAddExpenseShortcut(context: Context): Boolean {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TRANSACTION
            putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, "EXPENSE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pinShortcutInfo = ShortcutInfo.Builder(context, "shortcut_pinned_add_expense")
            .setShortLabel(context.getString(R.string.shortcut_add_transaction_short))
            .setLongLabel(context.getString(R.string.shortcut_add_transaction_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_add_expense))
            .setIntent(intent)
            .build()

        return shortcutManager.requestPinShortcut(pinShortcutInfo, null)
    }
}
