package com.anek.browser.browser

enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val homepageUrl: String
) {
    GOOGLE(
        "Google",
        "https://www.google.com/search?q=%s",
        "https://www.google.com"
    ),
    BING(
        "Bing",
        "https://www.bing.com/search?q=%s",
        "https://www.bing.com"
    ),
    DUCKDUCKGO(
        "DuckDuckGo",
        "https://duckduckgo.com/?q=%s",
        "https://duckduckgo.com"
    ),
    BRAVE(
        "Brave Search",
        "https://search.brave.com/search?q=%s",
        "https://search.brave.com"
    ),
    CUSTOM(
        "Custom",
        "%s",
        "https://www.google.com"
    );

    fun buildSearchUrl(query: String, customTemplate: String = ""): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return when (this) {
            CUSTOM -> {
                if (customTemplate.contains("%s")) {
                    customTemplate.replace("%s", encoded)
                } else {
                    // If no placeholder, append query
                    if (customTemplate.contains("?")) "$customTemplate$encoded"
                    else "$customTemplate?q=$encoded"
                }
            }
            else -> searchUrlTemplate.replace("%s", encoded)
        }
    }

    companion object {
        fun fromName(name: String): SearchEngine {
            return values().find { it.name == name } ?: GOOGLE
        }
    }
}
