package com.sans.finance.data.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "en" to "English",
            "id" to "Indonesia",
            "zh" to "中文 (Chinese)",
            "ja" to "日本語 (Japanese)",
            "ko" to "한국어 (Korean)",
            "fr" to "Français (French)",
            "de" to "Deutsch (German)",
            "es" to "Español (Spanish)",
            "ru" to "Русский (Russian)"
        )

        val COMMON_CURRENCIES = listOf(
            "USD",
            "IDR",
            "CNY",
            "EUR",
            "GBP",
            "JPY",
            "SGD",
            "AUD",
            "CAD",
            "CHF",
            "HKD",
            "KRW",
            "MYR",
            "PHP",
            "THB",
            "VND"
        )

        fun getAllAvailableCurrencies(): List<String> {
            return try {
                java.util.Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
            } catch (e: Exception) {
                COMMON_CURRENCIES
            }
        }
    }

    fun setLocale(language: String) {
        prefs.edit().putString("language", language).apply()

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val systemLocaleManager = context.getSystemService(android.app.LocaleManager::class.java)
        systemLocaleManager?.let {
            it.applicationLocales = android.os.LocaleList.forLanguageTags(language)
        }
    }

    fun getLocale(): String {
        val systemLocaleManager = context.getSystemService(android.app.LocaleManager::class.java)
        val currentLocale = systemLocaleManager?.applicationLocales?.let {
            if (it.isEmpty) null else it.get(0)
        }
        return currentLocale?.language ?: prefs.getString("language", "en") ?: "en"
    }

    fun updateResources(language: String) {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString("currency", currency).apply()
    }

    fun getCurrency(): String {
        return prefs.getString("currency", "IDR") ?: "IDR"
    }

    fun getEnabledCurrencies(): List<String> {
        val default = COMMON_CURRENCIES.take(6).joinToString(",")
        val currencies = prefs.getString("enabled_currencies", default) ?: default
        return currencies.split(",").filter { it.isNotBlank() }
    }

    fun setEnabledCurrencies(currencies: List<String>) {
        prefs.edit().putString("enabled_currencies", currencies.joinToString(",")).apply()
    }

    private val _privacyMode = kotlinx.coroutines.flow.MutableStateFlow(isPrivacyModeEnabled())
    val privacyMode: kotlinx.coroutines.flow.StateFlow<Boolean> = _privacyMode.asStateFlow()

    fun isPrivacyModeEnabled(): Boolean {
        return prefs.getBoolean("privacy_mode", false)
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_mode", enabled).apply()
        _privacyMode.value = enabled
    }

    // FIRE Settings
    private val _fireManualEnabled = kotlinx.coroutines.flow.MutableStateFlow(isFireManualEnabled())
    val fireManualEnabled = _fireManualEnabled.asStateFlow()

    private val _manualFireAnnualExpense =
        kotlinx.coroutines.flow.MutableStateFlow(getManualFireAnnualExpense())
    val manualFireAnnualExpense = _manualFireAnnualExpense.asStateFlow()

    fun isFireManualEnabled(): Boolean {
        return prefs.getBoolean("fire_manual_enabled", false)
    }

    fun setFireManualEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fire_manual_enabled", enabled).apply()
        _fireManualEnabled.value = enabled
    }

    fun getManualFireAnnualExpense(): Long {
        return prefs.getLong("fire_manual_annual_expense", 0L)
    }

    fun setManualFireAnnualExpense(amount: Long) {
        prefs.edit().putLong("fire_manual_annual_expense", amount).apply()
        _manualFireAnnualExpense.value = amount
    }

    // Backup & Sync Preferences (WhatsApp style)
    private val _backupFrequency = kotlinx.coroutines.flow.MutableStateFlow(getBackupFrequency())
    val backupFrequency = _backupFrequency.asStateFlow()

    private val _backupWifiOnly = kotlinx.coroutines.flow.MutableStateFlow(isBackupWifiOnly())
    val backupWifiOnly = _backupWifiOnly.asStateFlow()

    private val _backupRequiresCharging = kotlinx.coroutines.flow.MutableStateFlow(isBackupRequiresCharging())
    val backupRequiresCharging = _backupRequiresCharging.asStateFlow()

    private val _lastBackupTime = kotlinx.coroutines.flow.MutableStateFlow(getLastBackupTime())
    val lastBackupTime = _lastBackupTime.asStateFlow()

    private val _lastBackupSizeBytes = kotlinx.coroutines.flow.MutableStateFlow(getLastBackupSizeBytes())
    val lastBackupSizeBytes = _lastBackupSizeBytes.asStateFlow()

    private val _cloudBackupProvider = kotlinx.coroutines.flow.MutableStateFlow(getCloudBackupProvider())
    val cloudBackupProvider = _cloudBackupProvider.asStateFlow()

    fun getCloudBackupProvider(): String {
        return prefs.getString("cloud_backup_provider", "CLOUDFLARE_R2") ?: "CLOUDFLARE_R2"
    }

    fun setCloudBackupProvider(provider: String) {
        prefs.edit().putString("cloud_backup_provider", provider).apply()
        _cloudBackupProvider.value = provider
    }

    fun getR2AccountId(): String {
        return prefs.getString("r2_account_id", "") ?: ""
    }

    fun setR2AccountId(accountId: String) {
        prefs.edit().putString("r2_account_id", accountId.trim()).apply()
    }

    fun getR2AccessKeyId(): String {
        return prefs.getString("r2_access_key_id", "") ?: ""
    }

    fun setR2AccessKeyId(keyId: String) {
        prefs.edit().putString("r2_access_key_id", keyId.trim()).apply()
    }

    fun getR2SecretAccessKey(): String {
        return prefs.getString("r2_secret_access_key", "") ?: ""
    }

    fun setR2SecretAccessKey(secret: String) {
        prefs.edit().putString("r2_secret_access_key", secret.trim()).apply()
    }

    fun getR2BucketName(): String {
        return prefs.getString("r2_bucket_name", "ichsanul-dev") ?: "ichsanul-dev"
    }

    fun setR2BucketName(bucketName: String) {
        prefs.edit().putString("r2_bucket_name", bucketName.trim()).apply()
    }

    fun getGcsBucketName(): String {
        return prefs.getString("gcs_bucket_name", "ichsanul-portfolio-snapshots") ?: "ichsanul-portfolio-snapshots"
    }

    fun setGcsBucketName(bucketName: String) {
        prefs.edit().putString("gcs_bucket_name", bucketName.trim()).apply()
    }

    fun getBackupFrequency(): String {
        return prefs.getString("backup_frequency", "WEEKLY") ?: "WEEKLY"
    }

    fun setBackupFrequency(frequency: String) {
        prefs.edit().putString("backup_frequency", frequency).apply()
        _backupFrequency.value = frequency
    }

    fun isBackupWifiOnly(): Boolean {
        return prefs.getBoolean("backup_wifi_only", true)
    }

    fun setBackupWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean("backup_wifi_only", wifiOnly).apply()
        _backupWifiOnly.value = wifiOnly
    }

    fun isBackupRequiresCharging(): Boolean {
        return prefs.getBoolean("backup_requires_charging", true)
    }

    fun setBackupRequiresCharging(requiresCharging: Boolean) {
        prefs.edit().putBoolean("backup_requires_charging", requiresCharging).apply()
        _backupRequiresCharging.value = requiresCharging
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }

    fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong("last_backup_time", timestamp).apply()
        _lastBackupTime.value = timestamp
    }

    fun getLastBackupSizeBytes(): Long {
        return prefs.getLong("last_backup_size_bytes", 0L)
    }

    fun setLastBackupSizeBytes(bytes: Long) {
        prefs.edit().putLong("last_backup_size_bytes", bytes).apply()
        _lastBackupSizeBytes.value = bytes
    }
}
