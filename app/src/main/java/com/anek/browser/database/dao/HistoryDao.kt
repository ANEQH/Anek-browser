package com.anek.browser.database.dao

import androidx.room.*
import com.anek.browser.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getHistoryByDateRange(start: Long, end: Long): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("UPDATE history SET visitCount = visitCount + 1, timestamp = :timestamp, title = :title WHERE url = :url")
    suspend fun incrementVisitCount(url: String, title: String, timestamp: Long)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("DELETE FROM history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY position ASC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId IS :folderId ORDER BY position ASC")
    fun getBookmarksInFolder(folderId: Long?): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()

    @Query("SELECT * FROM bookmark_folders ORDER BY position ASC")
    fun getAllFolders(): Flow<List<BookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: BookmarkFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: BookmarkFolderEntity)

    @Query("DELETE FROM bookmark_folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY position ASC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: ShortcutEntity): Long

    @Update
    suspend fun update(shortcut: ShortcutEntity)

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shortcuts")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?
}
