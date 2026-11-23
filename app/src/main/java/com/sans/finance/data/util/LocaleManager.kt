package com.sans.finance.data.util

import android.content.Context
import android.content.SharedPreferences
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
        prefs.edit().putString("currency", currency).commit()
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
        prefs.edit().putString("enabled_currencies", currencies.joinToString(",")).commit()
    }
}
