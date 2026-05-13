package com.pathrift.anonve.android.app

import android.app.Application
import com.pathrift.anonve.android.core.ui.LanguageManager

class PathriftApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
    }
}
