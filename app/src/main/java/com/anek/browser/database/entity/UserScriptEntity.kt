package com.anek.browser.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user script — the Android-realistic stand-in for a browser extension.
 *
 * Chrome/Edge extensions cannot run in Android's WebView: there is no extension
 * API surface, no background page and no manifest loader. What *does* work is
 * injecting JavaScript into pages, which covers a large share of what people
 * actually use extensions for (tweaking layouts, removing elements, adding
 * buttons, dark modes, download helpers).
 */
@Entity(tableName = "user_scripts")
data class UserScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    /**
     * Comma / newline separated match rules. Each rule is one of:
     *  - `*`                     → every page
     *  - `example.com`           → that host and its subdomains
     *  - `https://example.com/a` → any URL starting with that prefix
     */
    val matchPattern: String = "*",
    val code: String,
    val enabled: Boolean = true,
    /** Inject after the page finishes rather than before it renders. */
    val runAtEnd: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0L
)
