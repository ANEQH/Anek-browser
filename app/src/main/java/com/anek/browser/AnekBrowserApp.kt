package com.anek.browser

import android.app.Application
import com.anek.browser.database.AppDatabase
import com.anek.browser.data.datastore.SettingsRepository
import com.anek.browser.web.WebViewWarmup

class AnekBrowserApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()

        /*
         * Load the WebView provider now, behind the splash screen, instead of on
         * the user's first tap. See WebViewWarmup for why a pooled instance is
         * deliberately not used.
         *
         * `WebView.setWebContentsDebuggingEnabled(true)` used to be called here
         * unconditionally. That made every release build inspectable by anyone
         * with a USB cable; it is now a Developer options toggle applied by
         * MainActivity from the persisted setting.
         */
        WebViewWarmup.warmUp(this)

        // Touch the database so the first Room query isn't paid on the UI thread.
        database
    }
}
