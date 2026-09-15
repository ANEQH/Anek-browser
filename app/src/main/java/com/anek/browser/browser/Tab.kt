package com.anek.browser.browser

import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "about:home",
    var title: String = "Home",
    var isIncognito: Boolean = false,
    var isDesktopMode: Boolean = false,
    var progress: Int = 0,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    var isLoading: Boolean = false,
    var favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessed: Long = System.currentTimeMillis(),
    var isPinned: Boolean = false
) {
    fun isHomePage(): Boolean = url == "about:home" || url == "anek://home" || url.isBlank()
    fun displayTitle(): String = when {
        isHomePage() -> "Home"
        title.isNotBlank() && title != url -> title
        else -> url
    }
    fun displayUrl(): String = if (isHomePage()) "" else url
    fun host(): String = try {
        java.net.URL(url).host ?: url
    } catch (_: Exception) { url }
}

data class ClosedTab(
    val tab: Tab,
    val closedAt: Long = System.currentTimeMillis()
)

// For persistence via DataStore JSON
@kotlinx.serialization.Serializable
data class PersistedTab(
    val id: String,
    val url: String,
    val title: String,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val createdAt: Long,
    val lastAccessed: Long
) {
    fun toTab(): Tab = Tab(
        id = id,
        url = url,
        title = title,
        isIncognito = isIncognito,
        isDesktopMode = isDesktopMode,
        createdAt = createdAt,
        lastAccessed = lastAccessed
    )
    companion object {
        fun fromTab(tab: Tab): PersistedTab = PersistedTab(
            id = tab.id,
            url = tab.url,
            title = tab.title,
            isIncognito = tab.isIncognito,
            isDesktopMode = tab.isDesktopMode,
            createdAt = tab.createdAt,
            lastAccessed = tab.lastAccessed
        )
    }
}
