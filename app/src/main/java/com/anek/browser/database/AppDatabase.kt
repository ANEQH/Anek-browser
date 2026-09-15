package com.anek.browser.database

import android.content.Context
import com.anek.browser.database.dao.*
import com.anek.browser.database.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory database implementation.
 * Room-ready architecture: can be swapped with Room implementation
 * by changing this file and adding Room dependencies + KSP.
 * This version is optimized for low-memory build environments.
 */
class AppDatabase private constructor(private val context: Context) {

    private val historyStorage = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private val bookmarkStorage = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private val folderStorage = MutableStateFlow<List<BookmarkFolderEntity>>(emptyList())
    private val shortcutStorage = MutableStateFlow<List<ShortcutEntity>>(emptyList())
    private val downloadStorage = MutableStateFlow<List<DownloadEntity>>(emptyList())

    private val historyDaoImpl = InMemoryHistoryDao(historyStorage)
    private val bookmarkDaoImpl = InMemoryBookmarkDao(bookmarkStorage, folderStorage)
    private val shortcutDaoImpl = InMemoryShortcutDao(shortcutStorage)
    private val downloadDaoImpl = InMemoryDownloadDao(downloadStorage)

    fun historyDao(): HistoryDao = historyDaoImpl
    fun bookmarkDao(): BookmarkDao = bookmarkDaoImpl
    fun shortcutDao(): ShortcutDao = shortcutDaoImpl
    fun downloadDao(): DownloadDao = downloadDaoImpl

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class InMemoryHistoryDao(
    private val storage: MutableStateFlow<List<HistoryEntity>>
) : HistoryDao {
    private var nextId = 1L

    override fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>> =
        storage.map { it.sortedByDescending { h -> h.timestamp }.take(limit) }

    override fun getAllHistory(): Flow<List<HistoryEntity>> =
        storage.map { it.sortedByDescending { h -> h.timestamp } }

    override suspend fun getHistoryByDateRange(start: Long, end: Long): List<HistoryEntity> =
        storage.value.filter { it.timestamp in start until end }.sortedByDescending { it.timestamp }

    override fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        storage.map { list ->
            list.filter { it.url.contains(query, true) || it.title.contains(query, true) }
                .sortedByDescending { it.timestamp }
        }

    override suspend fun getHistoryByUrl(url: String): HistoryEntity? =
        storage.value.find { it.url == url }

    override suspend fun insert(history: HistoryEntity): Long {
        val id = nextId++
        storage.value = storage.value + history.copy(id = id)
        return id
    }

    override suspend fun incrementVisitCount(url: String, title: String, timestamp: Long) {
        storage.value = storage.value.map {
            if (it.url == url) it.copy(visitCount = it.visitCount + 1, timestamp = timestamp, title = title)
            else it
        }
    }

    override suspend fun deleteById(id: Long) {
        storage.value = storage.value.filterNot { it.id == id }
    }

    override suspend fun deleteByUrl(url: String) {
        storage.value = storage.value.filterNot { it.url == url }
    }

    override suspend fun clearAll() {
        storage.value = emptyList()
    }

    override suspend fun deleteOlderThan(before: Long) {
        storage.value = storage.value.filter { it.timestamp >= before }
    }
}

class InMemoryBookmarkDao(
    private val bookmarkStorage: MutableStateFlow<List<BookmarkEntity>>,
    private val folderStorage: MutableStateFlow<List<BookmarkFolderEntity>>
) : BookmarkDao {
    private var nextId = 1L
    private var nextFolderId = 1L

    override fun getAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkStorage.map { it.sortedBy { b -> b.position } }

    override fun getBookmarksInFolder(folderId: Long?): Flow<List<BookmarkEntity>> =
        bookmarkStorage.map { list ->
            list.filter { it.folderId == folderId }.sortedBy { it.position }
        }

    override fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> =
        bookmarkStorage.map { list ->
            list.filter { it.url.contains(query, true) || it.title.contains(query, true) }
                .sortedByDescending { it.timestamp }
        }

    override suspend fun getBookmarkByUrl(url: String): BookmarkEntity? =
        bookmarkStorage.value.find { it.url == url }

    override suspend fun insert(bookmark: BookmarkEntity): Long {
        val id = nextId++
        bookmarkStorage.value = bookmarkStorage.value + bookmark.copy(id = id)
        return id
    }

    override suspend fun update(bookmark: BookmarkEntity) {
        bookmarkStorage.value = bookmarkStorage.value.map { if (it.id == bookmark.id) bookmark else it }
    }

    override suspend fun delete(bookmark: BookmarkEntity) {
        bookmarkStorage.value = bookmarkStorage.value.filterNot { it.id == bookmark.id }
    }

    override suspend fun deleteById(id: Long) {
        bookmarkStorage.value = bookmarkStorage.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        bookmarkStorage.value = emptyList()
    }

    override fun getAllFolders(): Flow<List<BookmarkFolderEntity>> =
        folderStorage.map { it.sortedBy { f -> f.position } }

    override suspend fun insertFolder(folder: BookmarkFolderEntity): Long {
        val id = nextFolderId++
        folderStorage.value = folderStorage.value + folder.copy(id = id)
        return id
    }

    override suspend fun updateFolder(folder: BookmarkFolderEntity) {
        folderStorage.value = folderStorage.value.map { if (it.id == folder.id) folder else it }
    }

    override suspend fun deleteFolder(folder: BookmarkFolderEntity) {
        folderStorage.value = folderStorage.value.filterNot { it.id == folder.id }
    }

    override suspend fun deleteFolderById(id: Long) {
        folderStorage.value = folderStorage.value.filterNot { it.id == id }
    }
}

class InMemoryShortcutDao(
    private val storage: MutableStateFlow<List<ShortcutEntity>>
) : ShortcutDao {
    private var nextId = 1L

    override fun getAllShortcuts(): Flow<List<ShortcutEntity>> =
        storage.map { it.sortedBy { s -> s.position } }

    override suspend fun insert(shortcut: ShortcutEntity): Long {
        val id = nextId++
        storage.value = storage.value + shortcut.copy(id = id)
        return id
    }

    override suspend fun update(shortcut: ShortcutEntity) {
        storage.value = storage.value.map { if (it.id == shortcut.id) shortcut else it }
    }

    override suspend fun delete(shortcut: ShortcutEntity) {
        storage.value = storage.value.filterNot { it.id == shortcut.id }
    }

    override suspend fun deleteById(id: Long) {
        storage.value = storage.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        storage.value = emptyList()
    }
}

class InMemoryDownloadDao(
    private val storage: MutableStateFlow<List<DownloadEntity>>
) : DownloadDao {
    private var nextId = 1L

    override fun getAllDownloads(): Flow<List<DownloadEntity>> =
        storage.map { it.sortedByDescending { d -> d.timestamp } }

    override suspend fun insert(download: DownloadEntity): Long {
        val id = nextId++
        storage.value = storage.value + download.copy(id = id)
        return id
    }

    override suspend fun update(download: DownloadEntity) {
        storage.value = storage.value.map { if (it.id == download.id) download else it }
    }

    override suspend fun deleteById(id: Long) {
        storage.value = storage.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        storage.value = emptyList()
    }

    override suspend fun getById(id: Long): DownloadEntity? =
        storage.value.find { it.id == id }
}
