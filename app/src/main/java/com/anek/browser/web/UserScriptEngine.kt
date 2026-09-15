package com.anek.browser.web

import android.net.Uri
import android.webkit.WebView
import com.anek.browser.database.dao.UserScriptDao
import com.anek.browser.database.entity.UserScriptEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Loads enabled user scripts and injects the ones that match the current URL.
 *
 * Chrome extensions cannot be installed in Android's WebView — there is no
 * extension runtime. Injecting page-scoped JavaScript is the supported way to
 * get the same class of customisation, so that is what this implements.
 */
class UserScriptEngine(
    private val dao: UserScriptDao,
    private val scope: CoroutineScope
) {

    /** Cached enabled scripts, refreshed from Room. Avoids a DB hit per page. */
    @Volatile
    private var cache: List<UserScriptEntity> = emptyList()

    fun startCaching() {
        scope.launch(Dispatchers.IO) {
            try {
                dao.getAll().collect { all -> cache = all.filter { it.enabled } }
            } catch (_: Throwable) {
                cache = emptyList()
            }
        }
    }

    fun scriptsFor(url: String): List<UserScriptEntity> {
        if (url.isBlank()) return emptyList()
        val snapshot = cache
        if (snapshot.isEmpty()) return emptyList()
        return snapshot.filter { matches(it.matchPattern, url) }
    }

    /**
     * Injects every matching script into [webView]. Each script runs inside its
     * own IIFE so one script's globals or syntax errors cannot take down the
     * others (or the page).
     *
     * @param atEnd when false, only scripts flagged to run early are injected.
     */
    fun inject(webView: WebView, url: String, atEnd: Boolean) {
        val matching = scriptsFor(url)
        if (matching.isEmpty()) return
        val toRun = matching.filter { it.runAtEnd == atEnd }
        if (toRun.isEmpty()) return

        val bundle = toRun.joinToString("\n") { script ->
            val safe = escapeForJsString(script.code)
            // Wrapped in try/catch + IIFE: isolate failures per script.
            """
            (function(){
              try {
                var __src = "$safe";
                (new Function(__src))();
              } catch (e) {
                if (window.console) console.error('[AnekScript:${escapeForJsString(script.name)}]', e);
              }
            })();
            """.trimIndent()
        }

        webView.evaluateJavascript(bundle, null)

        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            toRun.forEach { runCatching { dao.touchRun(it.id, now) } }
        }
    }

    companion object {

        /**
         * Match semantics:
         *  - blank or `*`            → all URLs
         *  - `example.com`           → host equals it or is a subdomain
         *  - `.example.com`          → subdomains only
         *  - anything with `://`     → URL prefix match
         *  - leading `/`             → path prefix match on the same request
         * Multiple rules may be separated by comma, whitespace or newline.
         */
        fun matches(pattern: String, url: String): Boolean {
            val rules = pattern.split(',', '\n', ' ', '\t', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (rules.isEmpty() || rules.any { it == "*" }) return true

            val uri = try { Uri.parse(url) } catch (_: Throwable) { return false }
            val host = uri.host?.lowercase() ?: ""

            for (rule in rules) {
                val r = rule.trim()
                if (r == "*") return true
                if (r.contains("://")) {
                    if (url.startsWith(r, ignoreCase = true)) return true
                    continue
                }
                if (r.startsWith("/")) {
                    if ((uri.path ?: "").startsWith(r, ignoreCase = true)) return true
                    continue
                }
                val domain = r.lowercase().removePrefix("www.")
                if (domain.startsWith(".")) {
                    if (host.endsWith(domain, ignoreCase = true)) return true
                } else {
                    val bareHost = host.removePrefix("www.")
                    if (bareHost == domain || bareHost.endsWith(".$domain")) return true
                }
            }
            return false
        }

        /**
         * Escapes arbitrary user JavaScript so it can be embedded inside a
         * double-quoted JS string literal in the generated bundle.
         */
        fun escapeForJsString(s: String): String {
            val sb = StringBuilder(s.length + 16)
            for (c in s) {
                when (c) {
                    '\\' -> sb.append("\\\\")
                    '"' -> sb.append("\\\"")
                    '\'' -> sb.append("\\'")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    // U+2028/U+2029 are line terminators in JS and would break
                    // the literal even though they are valid in Kotlin strings.
                    '\u2028' -> sb.append("\\u2028")
                    '\u2029' -> sb.append("\\u2029")
                    else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
