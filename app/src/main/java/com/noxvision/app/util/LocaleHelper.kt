package com.noxvision.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

object LocaleHelper {

    val AVAILABLE_LANGUAGES = listOf(
        LanguageOption("", "Systemsprache", "System"),
        LanguageOption("de", "Deutsch", "Deutsch"),
        LanguageOption("en", "Englisch", "English"),
        LanguageOption("fr", "Französisch", "Français"),
        LanguageOption("es", "Spanisch", "Español")
    )

    fun setLocale(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getSelectedLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            ""
        } else {
            locales.get(0)?.language ?: ""
        }
    }

    fun getSelectedLanguageDisplayName(): String {
        val code = getSelectedLanguageCode()
        return AVAILABLE_LANGUAGES.find { it.code == code }?.displayName
            ?: AVAILABLE_LANGUAGES.first().displayName
    }

    fun getAvailableLanguages(): List<LanguageOption> = AVAILABLE_LANGUAGES
}
