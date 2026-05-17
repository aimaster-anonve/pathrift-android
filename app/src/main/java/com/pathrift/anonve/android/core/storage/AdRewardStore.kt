package com.pathrift.anonve.android.core.storage

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdRewardStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pathrift_ad_rewards", Context.MODE_PRIVATE)

    val maxDailyAds = 3
    val rewardPerAd = 5

    private val todayKey: String
        get() = "ads_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val adsWatchedToday: Int
        get() = prefs.getInt(todayKey, 0)

    val canWatch: Boolean
        get() = adsWatchedToday < maxDailyAds

    val adsRemaining: Int
        get() = maxOf(0, maxDailyAds - adsWatchedToday)

    /** Simulate watching a rewarded ad. Returns earned diamonds, or 0 if limit reached. */
    fun watchAd(diamondStore: DiamondStore): Int {
        if (!canWatch) return 0
        prefs.edit().putInt(todayKey, adsWatchedToday + 1).apply()
        diamondStore.earn(rewardPerAd)
        return rewardPerAd
    }
}
