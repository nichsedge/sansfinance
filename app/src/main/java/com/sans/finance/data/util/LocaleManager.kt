package com.sans.finance.data.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class LocaleManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

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
        return prefs.getString("currency", "USD") ?: "USD"
    }

    fun getEnabledCurrencies(): List<String> {
        val currencies = prefs.getString("enabled_currencies", "USD,IDR,CNY") ?: "USD,IDR,CNY"
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
}
