package com.anek.browser.browser

import android.util.Patterns
import java.net.IDN
import java.net.URL
import java.util.regex.Pattern

object UrlUtils {

    private val IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(:\\d+)?(/.*)?$"
    )

    private val IPV6_PATTERN = Pattern.compile(
        "^\\[?[0-9a-fA-F:]+]?(:\\d+)?(/.*)?$"
    )

    private val LOCALHOST_PATTERN = Pattern.compile(
        "^(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0|10\\.0\\.2\\.2)(:\\d+)?(/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val DOMAIN_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(\\.[a-zA-Z]{2,})?(:\\d+)?(/.*)?$"
    )

    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        // Already has scheme
        if (trimmed.startsWith("http://", true) ||
            trimmed.startsWith("https://", true) ||
            trimmed.startsWith("file://") ||
            trimmed.startsWith("about:") ||
            trimmed.startsWith("data:") ||
            trimmed.startsWith("javascript:")
        ) {
            return true
        }

        // localhost or IP
        if (LOCALHOST_PATTERN.matcher(trimmed).matches()) return true
        if (IPV4_PATTERN.matcher(trimmed).matches()) return true
        // IPv6 is trickier, but check if contains colon and valid
        if (trimmed.contains(":") && trimmed.count { it == ':' } >= 2) {
            // Could be IPv6, treat as potential URL if no spaces
            if (!trimmed.contains(" ")) return true
        }

        // Contains spaces -> not URL, it's search
        if (trimmed.contains(" ")) return false

        // Check if looks like domain with TLD
        if (DOMAIN_PATTERN.matcher(trimmed).matches()) return true

        // Check with Patterns.WEB_URL but ensure it has dot and no spaces
        if (Patterns.WEB_URL.matcher(trimmed).matches() && trimmed.contains(".")) {
            // Additional check: shouldn't look like "something something"
            if (!trimmed.contains(" ")) return true
        }

        // If contains dot and no spaces and length > 3, likely URL
        if (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3) {
            val parts = trimmed.split(".")
            if (parts.size >= 2 && parts.last().length >= 2) {
                // Check TLD length 2-63 and alphabetic
                val tld = parts.last().substringBefore("/").substringBefore(":").substringBefore("?")
                if (tld.length in 2..63 && tld.all { it.isLetter() }) {
                    return true
                }
            }
        }

        return false
    }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Preserve special schemes
        if (trimmed.startsWith("about:") ||
            trimmed.startsWith("data:") ||
            trimmed.startsWith("javascript:") ||
            trimmed.startsWith("file://")
        ) {
            return trimmed
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        // For localhost, IP, add http://
        if (LOCALHOST_PATTERN.matcher(trimmed).matches() ||
            IPV4_PATTERN.matcher(trimmed).matches() ||
            trimmed.matches(Regex("^\\[.*].*"))
        ) {
            return "http://$trimmed"
        }

        // Otherwise assume https
        return "https://$trimmed"
    }

    fun resolveInputToUrl(
        input: String,
        searchEngine: SearchEngine,
        customSearchTemplate: String = ""
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Constants.HOME_PAGE_URL

        if (trimmed.equals("about:home", true) || trimmed.equals("anek://home", true)) {
            return Constants.HOME_PAGE_URL
        }

        return if (isValidUrl(trimmed)) {
            normalizeUrl(trimmed)
        } else {
            searchEngine.buildSearchUrl(trimmed, customSearchTemplate)
        }
    }

    fun getHost(url: String): String {
        return try {
            val parsed = URL(url)
            parsed.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    fun isSearchUrl(url: String, engine: SearchEngine): Boolean {
        return try {
            val host = URL(url).host
            engine.homepageUrl.contains(host) || url.contains("search?q=") || url.contains("/search?")
        } catch (_: Exception) {
            false
        }
    }

    fun toPunycode(url: String): String {
        return try {
            val parsed = URL(url)
            val host = parsed.host
            val asciiHost = IDN.toASCII(host)
            url.replace(host, asciiHost)
        } catch (_: Exception) {
            url
        }
    }
}

private object Constants {
    const val HOME_PAGE_URL = "about:home"
}
