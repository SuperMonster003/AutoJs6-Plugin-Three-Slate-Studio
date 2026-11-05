package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

/**
 * The storage permission of roadmap D9: all files access on Android 11+, the legacy storage permissions below. Paths in
 * the plugin's own folders (internal data, `Android/data/<package>`, `Android/media/<package>`) and paths outside shared
 * storage never need it; their ordinary file permissions decide.
 * zh-CN: 路线图 D9 的存储权限: Android 11 及以上为所有文件访问, 以下为旧版存储权限. 插件自己的目录 (内部数据,
 * `Android/data/<包名>`, `Android/media/<包名>`) 以及共享存储之外的路径不需要它, 由普通文件权限决定.
 */
internal object StorageAccess {

    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager()
        return listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Opens the all files access page of the plugin on Android 11+ (the list page on ROMs without it), otherwise asks for
     * the legacy permissions; the answer arrives in `onRequestPermissionsResult` with [requestCode].
     * zh-CN: Android 11 及以上打开插件的所有文件访问页 (不提供该页的 ROM 打开列表页), 以下申请旧版权限; 结果以 [requestCode]
     * 回到 `onRequestPermissionsResult`.
     */
    fun request(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appPage = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${activity.packageName}".toUri())
            try {
                activity.startActivity(appPage)
            } catch (_: ActivityNotFoundException) {
                // Some ROMs only offer the list page. zh-CN: 部分 ROM 只提供列表页.
                activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
        }
    }

    /** True when [path] lies in shared storage outside the plugin's own folders. zh-CN: [path] 位于共享存储且不在插件自己的目录中时为 true. */
    fun needsGrant(context: Context, path: String): Boolean {
        val target = canonical(File(path))
        if (ownFolders(context).any { target.isUnder(it) }) return false
        return sharedRoots().any { target.isUnder(it) }
    }

    /** [needsGrant] and not [isGranted]. zh-CN: [needsGrant] 且未 [isGranted]. */
    fun lacksAccess(context: Context, path: String): Boolean = needsGrant(context, path) && !isGranted(context)

    /**
     * True when [path] lies in a private folder of the plugin (internal storage, `Android/data`), which the media
     * scanner never indexes; `Android/media` is not private.
     * zh-CN: [path] 位于插件的私有目录 (内部存储, `Android/data`) 时为 true, 媒体扫描从不索引这些目录; `Android/media` 不算私有.
     */
    fun isPrivate(context: Context, path: String): Boolean {
        val target = canonical(File(path))
        return privateFolders(context).any { target.isUnder(it) }
    }

    private fun privateFolders(context: Context): List<String> {
        val app = context.applicationContext
        val folders = mutableListOf<File?>(app.filesDir.parentFile, app.cacheDir, app.filesDir)
        folders += runCatching { app.getExternalFilesDirs(null).toList() }.getOrDefault(emptyList()).map { it?.parentFile }
        folders += runCatching { app.externalCacheDirs.toList() }.getOrDefault(emptyList()).map { it?.parentFile }
        return folders.filterNotNull().map(::canonical).distinct()
    }

    private fun ownFolders(context: Context): List<String> {
        @Suppress("DEPRECATION")
        val media = runCatching { context.applicationContext.externalMediaDirs.toList() }.getOrDefault(emptyList())
        return (privateFolders(context) + media.filterNotNull().map(::canonical)).distinct()
    }

    // Scripts pass /sdcard paths, so the alias is matched as written, next to the canonical external storage path.
    @SuppressLint("SdCardPath")
    private fun sharedRoots(): List<String> {
        @Suppress("DEPRECATION")
        val external = Environment.getExternalStorageDirectory()
        return listOfNotNull(external, File("/storage"), File("/sdcard"), File("/mnt")).map(::canonical).distinct()
    }

    private fun canonical(file: File): String = try {
        file.canonicalPath
    } catch (_: IOException) {
        file.absolutePath
    }.trimEnd('/').ifEmpty { "/" }

    private fun String.isUnder(folder: String): Boolean = this == folder || startsWith(if (folder == "/") folder else "$folder/")

}
