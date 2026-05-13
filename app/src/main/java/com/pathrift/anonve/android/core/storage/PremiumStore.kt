package com.pathrift.anonve.android.core.storage

import android.content.Context

class PremiumStore(context: Context) {
    private val prefs = context.getSharedPreferences("pathrift_premium", Context.MODE_PRIVATE)
    private val key = "is_premium"

    var isPremium: Boolean
        get() = prefs.getBoolean(key, false)
        set(value) = prefs.edit().putBoolean(key, value).apply()

    fun activate() { isPremium = true }
}
