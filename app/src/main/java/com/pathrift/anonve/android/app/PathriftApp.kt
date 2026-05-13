package com.pathrift.anonve.android.app

import android.app.Application
import com.pathrift.anonve.android.core.storage.DiamondStore
import com.pathrift.anonve.android.core.ui.LanguageManager

class PathriftApp : Application() {

    lateinit var diamondStore: DiamondStore
        private set

    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
        diamondStore = DiamondStore(this)
    }
}
