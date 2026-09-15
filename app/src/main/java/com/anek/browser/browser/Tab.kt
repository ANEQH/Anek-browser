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
    var lastAccessed: Long = System.currentTimeMillis()
) {
    fun isHomePage(): Boolean = url == "about:home" || url == "anek://home" || url.isBlank()
}

data class ClosedTab(
    val tab: Tab,
    val closedAt: Long = System.currentTimeMillis()
)
