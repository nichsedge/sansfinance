package com.sans.finance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sans.finance.domain.preferences.BudgetPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class BudgetPreferencesImpl(
    private val context: Context
) : BudgetPreferences {

    companion object {
        private val MONTHLY_BUDGET_KEY = longPreferencesKey("monthly_budget")
    }

    override fun getMonthlyBudget(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[MONTHLY_BUDGET_KEY] ?: 0L
        }
    }

    override suspend fun setMonthlyBudget(amount: Long) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_BUDGET_KEY] = amount
        }
    }
}
