package com.anek.browser

import android.app.Application
import android.webkit.WebView
import com.anek.browser.database.AppDatabase
import com.anek.browser.data.datastore.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnekBrowserApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()

        try {
            WebView.setWebContentsDebuggingEnabled(true)
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Preload settings
                settingsRepository.settingsFlow.collect { _ -> }
            } catch (_: Exception) {}
        }
    }
}
