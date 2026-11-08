package com.sans.finance.domain.repository

import com.sans.finance.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setPrivacyModeEnabled(enabled: Boolean)
    suspend fun setFireManualEnabled(enabled: Boolean)
    suspend fun setManualFireAnnualExpense(amount: Long)
    suspend fun setBackupFrequency(frequency: String)
    suspend fun setBackupWifiOnly(wifiOnly: Boolean)
    suspend fun setBackupRequiresCharging(requiresCharging: Boolean)
    suspend fun setLastBackupTime(timestamp: Long)
    suspend fun setLastBackupSizeBytes(bytes: Long)
    suspend fun setCloudBackupProvider(provider: String)
    suspend fun setSyncGatewayConfig(url: String, token: String)
    suspend fun setR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String)
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}
