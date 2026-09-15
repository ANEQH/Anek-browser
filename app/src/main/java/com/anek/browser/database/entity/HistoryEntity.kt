package com.anek.browser.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val visitCount: Int = 1
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val folderId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val position: Int = 0
)

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val position: Int = 0
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val position: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String?,
    val mimeType: String?,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)
