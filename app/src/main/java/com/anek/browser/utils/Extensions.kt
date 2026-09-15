package com.anek.browser.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import java.text.SimpleDateFormat
import java.util.*

fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toTimeString(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDateTimeString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.isToday(): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    val today = Calendar.getInstance()
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun Long.isYesterday(): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
}

fun Context.shareText(text: String, title: String = "Share") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

fun Context.openUrlExternally(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (_: Exception) {}
}

fun String.isValidUrl(): Boolean {
    return URLUtil.isValidUrl(this)
}

fun String.guessFileName(contentDisposition: String?, mimeType: String?): String {
    return URLUtil.guessFileName(this, contentDisposition, mimeType)
}
