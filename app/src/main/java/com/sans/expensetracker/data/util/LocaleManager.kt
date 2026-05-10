package com.sans.expensetracker.data.util

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
}
