package com.anek.browser.database.dao

import com.anek.browser.database.entity.*
import kotlinx.coroutines.flow.Flow

interface HistoryDao {
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntity>>
    fun getAllHistory(): Flow<List<HistoryEntity>>
    suspend fun getHistoryByDateRange(start: Long, end: Long): List<HistoryEntity>
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    suspend fun getHistoryByUrl(url: String): HistoryEntity?
    suspend fun insert(history: HistoryEntity): Long
    suspend fun incrementVisitCount(url: String, title: String, timestamp: Long)
    suspend fun deleteById(id: Long)
    suspend fun deleteByUrl(url: String)
    suspend fun clearAll()
    suspend fun deleteOlderThan(before: Long)
}

interface BookmarkDao {
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    fun getBookmarksInFolder(folderId: Long?): Flow<List<BookmarkEntity>>
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?
    suspend fun insert(bookmark: BookmarkEntity): Long
    suspend fun update(bookmark: BookmarkEntity)
    suspend fun delete(bookmark: BookmarkEntity)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
    fun getAllFolders(): Flow<List<BookmarkFolderEntity>>
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long
    suspend fun updateFolder(folder: BookmarkFolderEntity)
    suspend fun deleteFolder(folder: BookmarkFolderEntity)
    suspend fun deleteFolderById(id: Long)
}

interface ShortcutDao {
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>
    suspend fun insert(shortcut: ShortcutEntity): Long
    suspend fun update(shortcut: ShortcutEntity)
    suspend fun delete(shortcut: ShortcutEntity)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}

interface DownloadDao {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    suspend fun insert(download: DownloadEntity): Long
    suspend fun update(download: DownloadEntity)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
    suspend fun getById(id: Long): DownloadEntity?
}
