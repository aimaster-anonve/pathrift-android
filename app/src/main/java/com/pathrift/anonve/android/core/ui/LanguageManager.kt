package com.pathrift.anonve.android.core.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    TURKISH("tr", "Türkçe", "🇹🇷");

    companion object {
        fun fromCode(code: String) = values().find { it.code == code } ?: ENGLISH
    }
}

object LanguageManager {
    private var prefs: SharedPreferences? = null
    private val _current = MutableStateFlow(AppLanguage.ENGLISH)
    val current: StateFlow<AppLanguage> = _current

    fun init(context: Context) {
        prefs = context.getSharedPreferences("pathrift_prefs", Context.MODE_PRIVATE)
        val saved = prefs?.getString("app_language", "en") ?: "en"
        _current.value = AppLanguage.fromCode(saved)
    }

    fun setLanguage(language: AppLanguage) {
        _current.value = language
        prefs?.edit()?.putString("app_language", language.code)?.apply()
    }

    fun s(en: String, tr: String): String =
        if (_current.value == AppLanguage.TURKISH) tr else en
}
