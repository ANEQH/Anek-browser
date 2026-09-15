package com.anek.browser.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import com.anek.browser.database.AppDatabase
import com.anek.browser.database.entity.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DownloadHandler {

    fun downloadFile(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        onStarted: (() -> Unit)? = null
    ) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading file...")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            // Save to DB
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                db.downloadDao().insert(
                    DownloadEntity(
                        url = url,
                        fileName = fileName,
                        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName,
                        mimeType = mimeType,
                        status = "RUNNING"
                    )
                )
            }

            onStarted?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
