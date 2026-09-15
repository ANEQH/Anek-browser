package com.anek.browser.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebView
import com.anek.browser.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Makes the first real WebView cheap.
 *
 * The very first `WebView(context)` in a process has to load the WebView
 * provider APK and `libwebviewchromium.so`, which routinely costs 300–800 ms on
 * mid-range devices. Doing that behind the splash screen instead of on the
 * user's first tap removes the visible stall.
 *
 * A `MutableContextWrapper`-based pool is deliberately *not* used here: a
 * pre-created WebView must be re-parented to an Activity before file choosers,
 * `alert()` dialogs and fullscreen video work correctly, and getting that wrong
 * produces much worse bugs than the few hundred milliseconds this saves.
 */
object WebViewWarmup {

    private val started = AtomicBoolean(false)

    /** `true` once [warmUp] has finished. */
    @Volatile
    var ready: Boolean = false
        private set

    /** Milliseconds the warm-up took, surfaced in Developer options. */
    @Volatile
    var warmupMillis: Long = -1L
        private set

    /**
     * Must be called on the main thread (a WebView can only be constructed on a
     * thread with a Looper). Safe to call repeatedly.
     */
    fun warmUp(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            val t0 = SystemClock.elapsedRealtime()
            try {
                // Forces the provider to load. Also initialises the cookie jar
                // and the default user-agent string, both of which are lazily
                // populated on first use.
                CookieManager.getInstance()
                android.webkit.WebSettings.getDefaultUserAgent(appContext)
                WebView(appContext).apply {
                    settings.javaScriptEnabled = false
                    destroy()
                }
            } catch (_: Throwable) {
                // A missing/broken WebView provider must never crash app start.
            } finally {
                warmupMillis = SystemClock.elapsedRealtime() - t0
                ready = true
            }
        }
    }

    /** WebView provider version string, or a readable fallback. */
    fun providerVersion(): String = try {
        WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" }
            ?: "Unknown"
    } catch (_: Throwable) {
        "Unknown"
    }

    /** `true` when the installed provider supports the modern dark-mode API. */
    fun supportsForceDark(): Boolean {
        return try {
            val name = WebView.getCurrentWebViewPackage()?.versionName
            if (name == null) return false
            val major = name.substringBefore('.').toIntOrNull() ?: 0
            major >= Constants.MIN_CHROME_MAJOR_FOR_FORCE_DARK
        } catch (_: Throwable) {
            false
        }
    }
}
