package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

import android.content.Context
import android.os.Environment
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Where `.slate.json` projects live (roadmap P4.3): `Movies/3-Slate Studio/Projects` beside the Studio videos (D37)
 * when the plugin may write shared storage, otherwise the plugin's private `files/projects`. Both are listed, newest
 * first.
 * zh-CN: `.slate.json` 工程的存放位置 (路线图 P4.3): 插件可写共享存储时在 Studio 视频旁的 `Movies/3-Slate Studio/Projects` (D37),
 * 否则在插件私有的 `files/projects`. 两处都会列出, 最新的在前.
 */
internal object ProjectStore {

    const val FOLDER = "Projects"

    data class Entry(val file: File, val name: String, val modified: Long, val shared: Boolean)

    @Suppress("DEPRECATION")
    fun sharedDirectory(): File = File(OutputPaths.defaultDirectory(Environment.getExternalStorageDirectory().path, OutputKind.VIDEO), FOLDER)

    fun privateDirectory(context: Context): File = File(context.filesDir, "projects")

    /** The folder new projects go to. zh-CN: 新工程的存放目录. */
    fun directory(context: Context): File = if (StorageAccess.isGranted(context)) sharedDirectory() else privateDirectory(context)

    fun list(context: Context): List<Entry> {
        val folders = buildList {
            if (StorageAccess.isGranted(context)) add(sharedDirectory() to true)
            add(privateDirectory(context) to false)
        }
        return folders.flatMap { (folder, shared) ->
            folder.listFiles { file -> file.isFile && file.name.endsWith(ProjectFiles.EXTENSION) }.orEmpty().map { file ->
                Entry(file, nameOf(file), file.lastModified(), shared)
            }
        }.sortedByDescending { it.modified }
    }

    /** The project name: the file name without `.slate.json`. zh-CN: 工程名: 去掉 `.slate.json` 的文件名. */
    fun nameOf(file: File): String = file.name.removeSuffix(ProjectFiles.EXTENSION)

    /** A new file name `Project yyyyMMdd-HHmmss.slate.json` that does not exist yet. zh-CN: 尚不存在的新文件名. */
    fun newFile(context: Context, now: Long = System.currentTimeMillis()): File {
        val folder = directory(context)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(now))
        val path = OutputPaths.uniquePath(File(folder, "project-$stamp${ProjectFiles.EXTENSION}").path) { File(it).exists() }
        return File(path)
    }

    /** Keeps a file name usable on shared storage. zh-CN: 保证文件名可用于共享存储. */
    fun safeName(name: String): String = name.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(MAX_NAME).ifEmpty { "project" }

    private const val MAX_NAME = 80

}
