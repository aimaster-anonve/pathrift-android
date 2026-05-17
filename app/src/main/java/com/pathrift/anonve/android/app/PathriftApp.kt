package com.pathrift.anonve.android.app

import android.app.Application
import com.pathrift.anonve.android.core.storage.AdRewardStore
import com.pathrift.anonve.android.core.storage.ArsenalStore
import com.pathrift.anonve.android.core.storage.DiamondStore
import com.pathrift.anonve.android.core.storage.GameSaveStore
import com.pathrift.anonve.android.core.storage.PremiumStore
import com.pathrift.anonve.android.core.ui.LanguageManager

class PathriftApp : Application() {

    lateinit var diamondStore: DiamondStore
        private set

    val premiumStore: PremiumStore by lazy { PremiumStore(this) }
    val arsenalStore: ArsenalStore by lazy { ArsenalStore(this) }
    val gameSaveStore: GameSaveStore by lazy { GameSaveStore(this) }
    val adRewardStore: AdRewardStore by lazy { AdRewardStore(this) }

    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
        // Pass premiumStore lazily — DiamondStore.isUnlocked will call premiumStore.isPremium
        diamondStore = DiamondStore(this, premiumStore)
    }
}
