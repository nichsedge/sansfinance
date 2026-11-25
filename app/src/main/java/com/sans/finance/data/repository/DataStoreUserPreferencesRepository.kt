package com.sans.finance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sans.finance.core.util.CryptoManager
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val BASE_CURRENCY = stringPreferencesKey("currency")
        val ENABLED_CURRENCIES = stringPreferencesKey("enabled_currencies")
        val FIRE_MANUAL_ENABLED = booleanPreferencesKey("fire_manual_enabled")
        val MANUAL_FIRE_ANNUAL_EXPENSE = longPreferencesKey("fire_manual_annual_expense")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val BACKUP_REQUIRES_CHARGING = booleanPreferencesKey("backup_requires_charging")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val LAST_BACKUP_SIZE_BYTES = longPreferencesKey("last_backup_size_bytes")
        val SYNC_GATEWAY_URL = stringPreferencesKey("sync_gateway_url")
        val SYNC_GATEWAY_TOKEN = stringPreferencesKey("sync_gateway_token")
        val EMAIL_SYNC_ENABLED = booleanPreferencesKey("email_sync_enabled")
        val EMAIL_SYNC_HOUR = intPreferencesKey("email_sync_hour")
        val EMAIL_SYNC_MINUTE = intPreferencesKey("email_sync_minute")
        val EMAIL_SYNC_WIFI_ONLY = booleanPreferencesKey("email_sync_wifi_only")
        val LAST_EMAIL_SYNC_TIME = longPreferencesKey("last_email_sync_time")
    }

    override val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val rawCurrencies = preferences[PreferencesKeys.ENABLED_CURRENCIES]
        val enabledList = if (!rawCurrencies.isNullOrBlank()) {
            rawCurrencies.split(",").filter { it.isNotBlank() }
        } else {
            com.sans.finance.data.util.LocaleManager.COMMON_CURRENCIES.take(6)
        }
        UserPreferences(
            isPrivacyModeEnabled = preferences[PreferencesKeys.PRIVACY_MODE] ?: false,
            baseCurrency = preferences[PreferencesKeys.BASE_CURRENCY] ?: "IDR",
            enabledCurrencies = enabledList,
            fireManualEnabled = preferences[PreferencesKeys.FIRE_MANUAL_ENABLED] ?: false,
            manualFireAnnualExpense = preferences[PreferencesKeys.MANUAL_FIRE_ANNUAL_EXPENSE] ?: 0L,
            backupFrequency = preferences[PreferencesKeys.BACKUP_FREQUENCY] ?: "WEEKLY",
            backupWifiOnly = preferences[PreferencesKeys.BACKUP_WIFI_ONLY] ?: true,
            backupRequiresCharging = preferences[PreferencesKeys.BACKUP_REQUIRES_CHARGING] ?: true,
            lastBackupTime = preferences[PreferencesKeys.LAST_BACKUP_TIME] ?: 0L,
            lastBackupSizeBytes = preferences[PreferencesKeys.LAST_BACKUP_SIZE_BYTES] ?: 0L,
            syncGatewayUrl = preferences[PreferencesKeys.SYNC_GATEWAY_URL] ?: "https://gmail-expenses-worker-886416340889.us-central1.run.app",
            syncGatewayToken = preferences[PreferencesKeys.SYNC_GATEWAY_TOKEN] ?: "",
            emailSyncEnabled = preferences[PreferencesKeys.EMAIL_SYNC_ENABLED] ?: true,
            emailSyncHour = preferences[PreferencesKeys.EMAIL_SYNC_HOUR] ?: 20,
            emailSyncMinute = preferences[PreferencesKeys.EMAIL_SYNC_MINUTE] ?: 0,
            emailSyncWifiOnly = preferences[PreferencesKeys.EMAIL_SYNC_WIFI_ONLY] ?: false,
            lastEmailSyncTime = preferences[PreferencesKeys.LAST_EMAIL_SYNC_TIME] ?: 0L
        )
    }

    override suspend fun setPrivacyModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_MODE] = enabled
        }
    }

    override suspend fun setBaseCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] = currency
        }
    }

    override suspend fun setEnabledCurrencies(currencies: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_CURRENCIES] = currencies.joinToString(",")
        }
    }

    override suspend fun setFireManualEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRE_MANUAL_ENABLED] = enabled
        }
    }

    override suspend fun setManualFireAnnualExpense(amount: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MANUAL_FIRE_ANNUAL_EXPENSE] = amount
        }
    }

    override suspend fun setBackupFrequency(frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_FREQUENCY] = frequency
        }
    }

    override suspend fun setBackupWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_WIFI_ONLY] = wifiOnly
        }
    }

    override suspend fun setBackupRequiresCharging(requiresCharging: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_REQUIRES_CHARGING] = requiresCharging
        }
    }

    override suspend fun setLastBackupTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = timestamp
        }
    }

    override suspend fun setLastBackupSizeBytes(bytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_SIZE_BYTES] = bytes
        }
    }

    override suspend fun setSyncGatewayConfig(url: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_GATEWAY_URL] = url
            preferences[PreferencesKeys.SYNC_GATEWAY_TOKEN] = token
        }
    }

    override suspend fun setEmailSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMAIL_SYNC_ENABLED] = enabled
        }
    }

    override suspend fun setEmailSyncTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMAIL_SYNC_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.EMAIL_SYNC_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    override suspend fun setEmailSyncWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMAIL_SYNC_WIFI_ONLY] = wifiOnly
        }
    }

    override suspend fun setLastEmailSyncTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_EMAIL_SYNC_TIME] = timestamp
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override suspend fun exportJson(): String {
        val prefs = userPreferences.first()
        return json.encodeToString(prefs)
    }

    override suspend fun importJson(json: String) {
        val prefs = this.json.decodeFromString<UserPreferences>(json)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_MODE] = prefs.isPrivacyModeEnabled
            preferences[PreferencesKeys.BASE_CURRENCY] = prefs.baseCurrency
            preferences[PreferencesKeys.ENABLED_CURRENCIES] = prefs.enabledCurrencies.joinToString(",")
            preferences[PreferencesKeys.FIRE_MANUAL_ENABLED] = prefs.fireManualEnabled
            preferences[PreferencesKeys.MANUAL_FIRE_ANNUAL_EXPENSE] = prefs.manualFireAnnualExpense
            preferences[PreferencesKeys.BACKUP_FREQUENCY] = prefs.backupFrequency
            preferences[PreferencesKeys.BACKUP_WIFI_ONLY] = prefs.backupWifiOnly
            preferences[PreferencesKeys.BACKUP_REQUIRES_CHARGING] = prefs.backupRequiresCharging
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = prefs.lastBackupTime
            preferences[PreferencesKeys.LAST_BACKUP_SIZE_BYTES] = prefs.lastBackupSizeBytes
            preferences[PreferencesKeys.SYNC_GATEWAY_URL] = prefs.syncGatewayUrl
            preferences[PreferencesKeys.SYNC_GATEWAY_TOKEN] = prefs.syncGatewayToken
            preferences[PreferencesKeys.EMAIL_SYNC_ENABLED] = prefs.emailSyncEnabled
            preferences[PreferencesKeys.EMAIL_SYNC_HOUR] = prefs.emailSyncHour
            preferences[PreferencesKeys.EMAIL_SYNC_MINUTE] = prefs.emailSyncMinute
            preferences[PreferencesKeys.EMAIL_SYNC_WIFI_ONLY] = prefs.emailSyncWifiOnly
            preferences[PreferencesKeys.LAST_EMAIL_SYNC_TIME] = prefs.lastEmailSyncTime
        }
    }
}
