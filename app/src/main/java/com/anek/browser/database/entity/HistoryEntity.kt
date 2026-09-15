package com.anek.browser.database.entity

data class HistoryEntity(
    val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val visitCount: Int = 1
)

data class BookmarkEntity(
    val id: Long = 0,
    val url: String,
    val title: String,
    val folderId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val position: Int = 0
)

data class BookmarkFolderEntity(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val position: Int = 0
)

data class ShortcutEntity(
    val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val position: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class DownloadEntity(
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String?,
    val mimeType: String?,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)
