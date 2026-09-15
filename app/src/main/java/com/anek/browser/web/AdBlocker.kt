package com.anek.browser.web

import android.net.Uri
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight, allocation-cheap request filter used from
 * `WebViewClient.shouldInterceptRequest`.
 *
 * This is the practical equivalent of an ad-blocking "extension" on Android:
 * the platform WebView cannot load Chrome extensions at all, so blocking has to
 * happen at the network-request layer instead.
 *
 * Matching is done on the request host and path against a small built-in list
 * plus any user supplied domains. Everything is a plain `HashSet` lookup or a
 * suffix match, so the per-request cost stays in the microseconds.
 */
object AdBlocker {

    /** Total requests blocked since process start (shown in the UI). */
    val blockedCount = AtomicInteger(0)

    /** Ad / analytics / tracker domains. Deliberately conservative: only
     *  domains whose entire purpose is advertising or cross-site tracking are
     *  listed, so blocking them cannot break first-party site functionality. */
    private val AD_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "adsystem.com",
        "adnxs.com",
        "adsrvr.org",
        "adform.net",
        "adcolony.com",
        "admob.com",
        "amazon-adsystem.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "moatads.com",
        "smartadserver.com",
        "yieldmo.com",
        "media.net",
        "inmobi.com",
        "unityads.unity3d.com",
        "applovin.com",
        "chartboost.com",
        "vungle.com",
        "ironsrc.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "revcontent.com",
        "zedo.com"
    )

    /** Pure third-party trackers. */
    private val TRACKER_DOMAINS = hashSetOf(
        "facebook.net",
        "connect.facebook.net",
        "graph.facebook.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "fullstory.com",
        "mouseflow.com",
        "crazyegg.com",
        "quantserve.com",
        "scorecardresearch.com",
        "bluekai.com",
        "demdex.net",
        "krxd.net",
        "rlcdn.com",
        "exelator.com",
        "tapad.com",
        "liveramp.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "firebase-settings.crashlytics.com",
        "bugsnag.com",
        "sentry.io",
        "newrelic.com",
        "nr-data.net",
        "onetrust.com",
        "trustarc.com",
        "adsymptotic.com"
    )

    /** Path fragments that nearly always indicate an ad slot or tracking pixel. */
    private val PATH_HINTS = arrayOf(
        "/ads.js", "/adsense", "/adserver", "/adserve", "/adframe",
        "/adsbygoogle", "/doubleclick", "/googlesyndication",
        "/pixel.gif", "/tracking-pixel", "/track.gif",
        "/popunder", "/popup.js", "/banner_ad", "/bannerads"
    )

    private val EMPTY_RESPONSE_BODY = ByteArray(0)

    /**
     * @return true when the request should be short-circuited with an empty
     *         response.
     */
    fun shouldBlock(
        url: String,
        blockAds: Boolean,
        blockTrackers: Boolean,
        customDomains: Set<String> = emptySet()
    ): Boolean {
        if (!blockAds && !blockTrackers && customDomains.isEmpty()) return false
        if (url.isEmpty()) return false

        val host = try {
            Uri.parse(url).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }
        if (host.isEmpty()) return false

        // User rules win over everything, including allow-listing a built-in
        // domain by prefixing it with '@'.
        for (rule in customDomains) {
            val r = rule.trim().lowercase()
            if (r.isEmpty()) continue
            if (r.startsWith("@")) {
                if (hostMatches(host, r.substring(1))) return false
            } else if (hostMatches(host, r)) {
                blockedCount.incrementAndGet()
                return true
            }
        }

        if (blockAds && hostMatchesAny(host, AD_DOMAINS)) {
            blockedCount.incrementAndGet()
            return true
        }
        if (blockTrackers && hostMatchesAny(host, TRACKER_DOMAINS)) {
            blockedCount.incrementAndGet()
            return true
        }

        val lower = url.lowercase()
        if (blockAds && PATH_HINTS.any { lower.contains(it) }) {
            blockedCount.incrementAndGet()
            return true
        }
        return false
    }

    /** True when [host] equals [domain] or is a subdomain of it. */
    private fun hostMatches(host: String, domain: String): Boolean {
        if (host == domain) return true
        return host.endsWith(".$domain")
    }

    private fun hostMatchesAny(host: String, domains: Set<String>): Boolean {
        for (d in domains) {
            if (host == d || host.endsWith(".$d")) return true
        }
        return false
    }

    /**
     * An empty 204-style response. Returning this instead of `null` prevents
     * the network request from ever being issued, which is what makes blocking
     * cheap.
     */
    fun emptyResponse(url: String): android.webkit.WebResourceResponse {
        return android.webkit.WebResourceResponse(
            "text/plain",
            "utf-8",
            204,
            "No Content",
            mapOf("Access-Control-Allow-Origin" to "*", "Content-Length" to "0"),
            EMPTY_RESPONSE_BODY.inputStream()
        )
    }

    fun resetCounter() {
        blockedCount.set(0)
    }

    fun parseCustomList(raw: String): Set<String> =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
            .toSet()

    val builtinRuleCount: Int get() = AD_DOMAINS.size + TRACKER_DOMAINS.size + PATH_HINTS.size
}
