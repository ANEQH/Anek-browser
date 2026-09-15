package com.anek.browser.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.StatFs
import java.io.File

/** Device / runtime facts shown in Developer options. */
object DeviceInfo {

    fun appVersionName(context: Context): String = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "?"
    } catch (_: Throwable) { "?" }

    fun appVersionCode(context: Context): Long = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } catch (_: Throwable) { -1L }

    fun isDebugBuild(context: Context): Boolean =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun androidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun deviceModel(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim() + (Build.DEVICE?.let { " / $it" } ?: "")

    fun totalRamMb(context: Context): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.totalMem / (1024 * 1024)
        } catch (_: Throwable) { -1L }
    }

    fun jvmHeapUsedMb(): String {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val max = rt.maxMemory() / (1024 * 1024)
        return "$used MB / $max MB"
    }

    fun nativeHeapMb(): String {
        val used = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        return "$used MB"
    }

    fun dirSizeMb(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        return try {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)
        } catch (_: Throwable) { 0L }
    }

    fun freeStorageMb(context: Context): Long = try {
        val stat = StatFs(context.filesDir.absolutePath)
        stat.availableBytes / (1024 * 1024)
    } catch (_: Throwable) { -1L }
}
