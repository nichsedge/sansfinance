package com.sans.finance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import com.sans.finance.core.util.CryptoManager
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "app_prefs"))
    }
)

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val FIRE_MANUAL_ENABLED = booleanPreferencesKey("fire_manual_enabled")
        val MANUAL_FIRE_ANNUAL_EXPENSE = longPreferencesKey("fire_manual_annual_expense")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val BACKUP_REQUIRES_CHARGING = booleanPreferencesKey("backup_requires_charging")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val LAST_BACKUP_SIZE_BYTES = longPreferencesKey("last_backup_size_bytes")
        val CLOUD_BACKUP_PROVIDER = stringPreferencesKey("cloud_backup_provider")
        val R2_ACCOUNT_ID = stringPreferencesKey("r2_account_id")
        val R2_ACCESS_KEY_ID = stringPreferencesKey("r2_access_key_id")
        val R2_SECRET_ACCESS_KEY = stringPreferencesKey("r2_secret_access_key")
        val R2_BUCKET_NAME = stringPreferencesKey("r2_bucket_name")
        val GCS_BUCKET_NAME = stringPreferencesKey("gcs_bucket_name")
    }

    override val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            isPrivacyModeEnabled = preferences[PreferencesKeys.PRIVACY_MODE] ?: false,
            fireManualEnabled = preferences[PreferencesKeys.FIRE_MANUAL_ENABLED] ?: false,
            manualFireAnnualExpense = preferences[PreferencesKeys.MANUAL_FIRE_ANNUAL_EXPENSE] ?: 0L,
            backupFrequency = preferences[PreferencesKeys.BACKUP_FREQUENCY] ?: "WEEKLY",
            backupWifiOnly = preferences[PreferencesKeys.BACKUP_WIFI_ONLY] ?: true,
            backupRequiresCharging = preferences[PreferencesKeys.BACKUP_REQUIRES_CHARGING] ?: true,
            lastBackupTime = preferences[PreferencesKeys.LAST_BACKUP_TIME] ?: 0L,
            lastBackupSizeBytes = preferences[PreferencesKeys.LAST_BACKUP_SIZE_BYTES] ?: 0L,
            cloudBackupProvider = preferences[PreferencesKeys.CLOUD_BACKUP_PROVIDER] ?: "CLOUDFLARE_R2",
            r2AccountId = preferences[PreferencesKeys.R2_ACCOUNT_ID] ?: "",
            r2AccessKeyId = preferences[PreferencesKeys.R2_ACCESS_KEY_ID] ?: "",
            r2SecretAccessKey = CryptoManager.decrypt(preferences[PreferencesKeys.R2_SECRET_ACCESS_KEY] ?: ""),
            r2BucketName = preferences[PreferencesKeys.R2_BUCKET_NAME] ?: "ichsanul-dev",
            gcsBucketName = preferences[PreferencesKeys.GCS_BUCKET_NAME] ?: "ichsanul-portfolio-snapshots"
        )
    }

    override suspend fun setPrivacyModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_MODE] = enabled
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

    override suspend fun setCloudBackupProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_BACKUP_PROVIDER] = provider
        }
    }

    override suspend fun setR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.R2_ACCOUNT_ID] = accountId
            preferences[PreferencesKeys.R2_ACCESS_KEY_ID] = accessKeyId
            preferences[PreferencesKeys.R2_SECRET_ACCESS_KEY] = CryptoManager.encrypt(secretAccessKey)
            preferences[PreferencesKeys.R2_BUCKET_NAME] = bucketName
        }
    }

    override suspend fun setGcsBucketName(bucketName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GCS_BUCKET_NAME] = bucketName
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
            preferences[PreferencesKeys.FIRE_MANUAL_ENABLED] = prefs.fireManualEnabled
            preferences[PreferencesKeys.MANUAL_FIRE_ANNUAL_EXPENSE] = prefs.manualFireAnnualExpense
            preferences[PreferencesKeys.BACKUP_FREQUENCY] = prefs.backupFrequency
            preferences[PreferencesKeys.BACKUP_WIFI_ONLY] = prefs.backupWifiOnly
            preferences[PreferencesKeys.BACKUP_REQUIRES_CHARGING] = prefs.backupRequiresCharging
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = prefs.lastBackupTime
            preferences[PreferencesKeys.LAST_BACKUP_SIZE_BYTES] = prefs.lastBackupSizeBytes
            preferences[PreferencesKeys.CLOUD_BACKUP_PROVIDER] = prefs.cloudBackupProvider
            preferences[PreferencesKeys.R2_ACCOUNT_ID] = prefs.r2AccountId
            preferences[PreferencesKeys.R2_ACCESS_KEY_ID] = prefs.r2AccessKeyId
            preferences[PreferencesKeys.R2_SECRET_ACCESS_KEY] = CryptoManager.encrypt(prefs.r2SecretAccessKey)
            preferences[PreferencesKeys.R2_BUCKET_NAME] = prefs.r2BucketName
            preferences[PreferencesKeys.GCS_BUCKET_NAME] = prefs.gcsBucketName
        }
    }
}
