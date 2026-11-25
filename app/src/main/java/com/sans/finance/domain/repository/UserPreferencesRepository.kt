package com.sans.finance.domain.repository

import com.sans.finance.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setBaseCurrency(currency: String)
    suspend fun setEnabledCurrencies(currencies: List<String>)
    suspend fun setPrivacyModeEnabled(enabled: Boolean)
    suspend fun setFireManualEnabled(enabled: Boolean)
    suspend fun setManualFireAnnualExpense(amount: Long)
    suspend fun setBackupFrequency(frequency: String)
    suspend fun setBackupWifiOnly(wifiOnly: Boolean)
    suspend fun setBackupRequiresCharging(requiresCharging: Boolean)
    suspend fun setLastBackupTime(timestamp: Long)
    suspend fun setLastBackupSizeBytes(bytes: Long)
    suspend fun setSyncGatewayConfig(url: String, token: String)
    suspend fun setEmailSyncEnabled(enabled: Boolean)
    suspend fun setEmailSyncTime(hour: Int, minute: Int)
    suspend fun setEmailSyncWifiOnly(wifiOnly: Boolean)
    suspend fun setLastEmailSyncTime(timestamp: Long)
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}
